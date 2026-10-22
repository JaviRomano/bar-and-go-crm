package com.barandgo.crm.recipes.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.catalog.ProductDTO;
import com.barandgo.crm.catalog.ProductService;
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.inventory.MovementSource;
import com.barandgo.crm.inventory.MovementType;
import com.barandgo.crm.inventory.StockPostingService;
import com.barandgo.crm.inventory.StockPostingService.PostingLine;
import com.barandgo.crm.inventory.StockPostingService.PostingResult;
import com.barandgo.crm.inventory.InventoryDTOs.StockMovementDTO;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.recipes.RecipeDTOs;
import com.barandgo.crm.recipes.RecipeDTOs.CostHistoryEntry;
import com.barandgo.crm.recipes.RecipeDTOs.LineRequest;
import com.barandgo.crm.recipes.RecipeDTOs.ProductionDTO;
import com.barandgo.crm.recipes.RecipeDTOs.ProductionRequest;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeDTO;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeRequest;
import com.barandgo.crm.recipes.RecipeService;
import com.barandgo.crm.recipes.RecipeType;
import com.barandgo.crm.recipes.internal.RecipeCostHistory.Reason;
import com.barandgo.crm.recipes.internal.RecipeCosting.RecipeCost;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;

@Service
@Transactional
public class RecipeServiceImpl implements RecipeService {

    private static final Logger logger = LoggerFactory.getLogger(RecipeServiceImpl.class);
    private static final BigDecimal FULL_YIELD = new BigDecimal("100");

    private final RecipeRepository recipeRepository;
    private final RecipeCostHistoryRepository historyRepository;
    private final ProductionBatchRepository productionRepository;
    private final RecipeModelLoader modelLoader;
    private final ProductService productService;
    private final StockPostingService stockPosting;
    private final BigDecimal defaultTargetFoodCostPct;

    public RecipeServiceImpl(RecipeRepository recipeRepository, RecipeCostHistoryRepository historyRepository,
                             ProductionBatchRepository productionRepository, RecipeModelLoader modelLoader,
                             ProductService productService, StockPostingService stockPosting,
                             @Value("${app.recipes.target-food-cost-pct}") BigDecimal defaultTargetFoodCostPct) {
        this.recipeRepository = recipeRepository;
        this.historyRepository = historyRepository;
        this.productionRepository = productionRepository;
        this.modelLoader = modelLoader;
        this.productService = productService;
        this.stockPosting = stockPosting;
        this.defaultTargetFoodCostPct = defaultTargetFoodCostPct;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDTO> getRecipes(RecipeType type, CurrentUser requester) {
        RecipeModelLoader.Model model = modelLoader.load(showsCosts(requester));
        // La carta de una vez; un producto archivado (no está en ella) se pide aparte
        Map<Long, ProductDTO> products = productService.getAllProducts().stream()
                .collect(Collectors.toMap(ProductDTO::getId, product -> product, (a, b) -> a, HashMap::new));
        return model.recipes().values().stream()
                .filter(recipe -> type == null || type == recipe.getType())
                .sorted(Comparator.comparing(Recipe::getName))
                .map(recipe -> toDTO(recipe, model, products, requester))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDTO getRecipe(Long id, CurrentUser requester) {
        findRecipe(id);
        RecipeModelLoader.Model model = modelLoader.load(showsCosts(requester));
        return toDTO(model.recipes().get(id), model, new HashMap<>(), requester);
    }

    @Override
    public RecipeDTO createRecipe(RecipeRequest request, CurrentUser requester) {
        Recipe recipe = new Recipe(request.type());
        apply(recipe, request);
        Recipe saved = recipeRepository.save(recipe);
        recordCost(saved.getId(), Reason.CREATED);

        logger.info("Escandallo {} '{}' creado", saved.getId(), saved.getName());
        return getRecipe(saved.getId(), requester);
    }

    @Override
    public RecipeDTO updateRecipe(Long id, RecipeRequest request, CurrentUser requester) {
        Recipe recipe = findRecipe(id);
        if (request.type() != recipe.getType()) {
            throw new BadRequestException("El tipo de un escandallo no cambia: crea uno nuevo");
        }
        apply(recipe, request);
        recipeRepository.save(recipe);
        recordCost(id, Reason.UPDATED);

        logger.info("Escandallo {} actualizado", id);
        return getRecipe(id, requester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostHistoryEntry> getCostHistory(Long id) {
        findRecipe(id);
        return historyRepository.findByRecipeIdOrderByRecordedAtDescIdDesc(id).stream()
                .map(h -> new CostHistoryEntry(h.getTotalCost(), h.getCostPerUnit(), h.getComplete(),
                        h.getReason().name(), h.getRecordedAt()))
                .toList();
    }

    @Override
    public int recalculateAll() {
        RecipeModelLoader.Model model = modelLoader.load(true);
        int changed = 0;
        for (Long recipeId : model.recipes().keySet()) {
            if (recordIfChanged(recipeId, model.costing().cost(recipeId), Reason.RECALCULATION)) {
                changed++;
            }
        }
        logger.info("Recálculo de escandallos: {} con coste distinto", changed);
        return changed;
    }

    /**
     * Recalcula los escandallos que usan el artículo, directamente o a través de sub-escandallos.
     * Lo invoca el listener de cambios de coste de compras.
     */
    public void recalculateAffectedBy(Long stockItemId) {
        RecipeModelLoader.Model model = modelLoader.load(true);
        Set<Long> affected = new HashSet<>();
        model.recipes().values().stream()
                .filter(r -> r.getLines().stream().anyMatch(l -> stockItemId.equals(l.getStockItemId())))
                .forEach(r -> collectWithParents(r.getId(), model, affected));
        affected.forEach(recipeId -> recordIfChanged(recipeId, model.costing().cost(recipeId), Reason.PRICE_CHANGE));
        if (!affected.isEmpty()) {
            logger.info("Cambio de coste del artículo {}: {} escandallos revisados", stockItemId, affected.size());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<Long, BigDecimal>> consumptionForProduct(Long productId, BigDecimal portions) {
        Optional<Recipe> recipe = recipeRepository.findByProductId(productId)
                .filter(r -> Boolean.TRUE.equals(r.getActive()));
        if (recipe.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(modelLoader.load(false).costing().stockConsumption(recipe.get().getId(), portions));
    }

    @Override
    public ProductionDTO produce(Long id, ProductionRequest request, CurrentUser requester) {
        Recipe recipe = findRecipe(id);
        if (recipe.getType() != RecipeType.PREPARATION || recipe.getStockItemId() == null) {
            throw new BadRequestException("Solo se producen elaboraciones con artículo de stock asociado");
        }
        if (!Boolean.TRUE.equals(recipe.getActive())) {
            throw new BadRequestException("El escandallo '" + recipe.getName() + "' está desactivado");
        }
        RecipeModelLoader.Model model = modelLoader.load(false);
        String yieldUnit = model.unitCode(recipe.getYieldUnitId());
        UnitConverter outputConverter = model.outputConverterOf(recipe);
        BigDecimal quantity = request.unit() == null || request.unit().equals(yieldUnit)
                ? request.quantity()
                : outputConverter.convert(request.quantity(), request.unit(), yieldUnit);

        UUID operationId = UUID.randomUUID();
        List<PostingLine> consumption = model.costing().stockConsumption(id, quantity).entrySet().stream()
                .filter(entry -> entry.getValue().signum() > 0)
                .map(entry -> new PostingLine(entry.getKey(), entry.getValue().negate(), null))
                .toList();
        PostingResult consumed = stockPosting.post(operationId, MovementType.PRODUCTION_CONSUMPTION,
                MovementSource.PRODUCTION, id, request.warehouseId(), consumption, request.notes(), requester);

        // La elaboración entra al coste real de lo consumido (PMP), no al teórico del escandallo
        StockItemDTO output = model.items().get(recipe.getStockItemId());
        BigDecimal outputQuantity = model.converterOf(output).convert(quantity, yieldUnit, output.stockUnit())
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalCost = consumed.totalCost().negate();
        BigDecimal unitCost = totalCost.divide(outputQuantity, 6, RoundingMode.HALF_UP);
        PostingResult produced = stockPosting.post(operationId, MovementType.PRODUCTION_OUTPUT,
                MovementSource.PRODUCTION, id, request.warehouseId(),
                List.of(new PostingLine(output.id(), outputQuantity, unitCost)), request.notes(), requester);

        ProductionBatch batch = productionRepository.save(new ProductionBatch(id, request.warehouseId(), quantity,
                recipe.getYieldUnitId(), totalCost.setScale(4, RoundingMode.HALF_UP), unitCost, operationId,
                request.notes(), requester.id()));

        List<StockMovementDTO> movements = new ArrayList<>(consumed.movements());
        movements.addAll(produced.movements());
        logger.info("Producción {} de '{}': {} {} a {} por {}", batch.getId(), recipe.getName(), quantity, yieldUnit,
                unitCost, output.stockUnit());
        return new ProductionDTO(batch.getId(), id, recipe.getName(), request.warehouseId(), quantity, yieldUnit,
                totalCost.setScale(4, RoundingMode.HALF_UP), unitCost, operationId, movements);
    }

    /** Valida la petición y la copia al escandallo. */
    private void apply(Recipe recipe, RecipeRequest request) {
        String name = request.name().trim();
        boolean duplicated = recipe.getId() == null
                ? recipeRepository.existsByNameIgnoreCase(name)
                : recipeRepository.existsByNameIgnoreCaseAndIdNot(name, recipe.getId());
        if (duplicated) {
            throw new ConflictException("Ya existe un escandallo llamado '" + name + "'");
        }
        RecipeModelLoader.Model model = modelLoader.load(false);

        if (request.type() == RecipeType.DISH) {
            applyDish(recipe, request);
        } else {
            applyPreparation(recipe, request, model);
        }
        recipe.setName(name);
        recipe.setTargetFoodCostPct(request.targetFoodCostPct());
        recipe.setNotes(request.notes());
        if (request.active() != null) {
            recipe.setActive(request.active());
        }

        List<RecipeLine> lines = new ArrayList<>();
        int lineNumber = 1;
        for (LineRequest line : request.lines()) {
            lines.add(buildLine(recipe, line, lineNumber++, model));
        }
        if (recipe.getId() != null) {
            rejectCycles(recipe, lines, model);
        }
        recipe.replaceLines(lines);
    }

    private void applyDish(Recipe recipe, RecipeRequest request) {
        if (request.productId() == null || request.portions() == null) {
            throw new BadRequestException("Un plato necesita producto y número de raciones");
        }
        if (request.stockItemId() != null) {
            throw new BadRequestException("Un plato no tiene artículo de stock: se vende, no se guarda");
        }
        productService.getProductById(request.productId());
        boolean productTaken = recipe.getId() == null
                ? recipeRepository.findByProductId(request.productId()).isPresent()
                : recipeRepository.existsByProductIdAndIdNot(request.productId(), recipe.getId());
        if (productTaken) {
            throw new ConflictException("El producto " + request.productId() + " ya tiene escandallo");
        }
        recipe.setProductId(request.productId());
        recipe.setPortions(request.portions());
    }

    private void applyPreparation(Recipe recipe, RecipeRequest request, RecipeModelLoader.Model model) {
        if (request.productId() != null) {
            throw new BadRequestException("Una elaboración no es un producto de la carta");
        }
        if (request.yieldQuantity() == null || request.yieldUnit() == null) {
            throw new BadRequestException("Una elaboración necesita cantidad y unidad de rendimiento");
        }
        Long yieldUnitId = unitId(request.yieldUnit(), model);
        if (request.stockItemId() != null) {
            StockItemDTO item = Optional.ofNullable(model.items().get(request.stockItemId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + request.stockItemId()));
            boolean taken = model.recipes().values().stream()
                    .anyMatch(r -> request.stockItemId().equals(r.getStockItemId()) && !Objects.equals(r.getId(), recipe.getId()));
            if (taken) {
                throw new ConflictException("El artículo " + item.code() + " ya es el resultado de otra elaboración");
            }
            if (!model.converterOf(item).canConvert(request.yieldUnit(), item.stockUnit())) {
                throw new BadRequestException("El rendimiento en '" + request.yieldUnit()
                        + "' no se puede pasar a la unidad de inventario de " + item.code() + " ('" + item.stockUnit() + "')");
            }
        }
        recipe.setStockItemId(request.stockItemId());
        recipe.setYieldQuantity(request.yieldQuantity());
        recipe.setYieldUnitId(yieldUnitId);
    }

    private RecipeLine buildLine(Recipe recipe, LineRequest line, int lineNumber, RecipeModelLoader.Model model) {
        if ((line.stockItemId() == null) == (line.subRecipeId() == null)) {
            throw new BadRequestException("Cada ingrediente es un artículo o una elaboración, no las dos cosas ni ninguna");
        }
        Long unitId = unitId(line.unit(), model);
        BigDecimal yieldPct = line.yieldPct() != null ? line.yieldPct() : FULL_YIELD;

        if (line.stockItemId() != null) {
            StockItemDTO item = Optional.ofNullable(model.items().get(line.stockItemId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + line.stockItemId()));
            if (!model.converterOf(item).canConvert(line.unit(), item.stockUnit())) {
                throw new BadRequestException("'" + line.unit() + "' no se puede pasar a la unidad de " + item.code()
                        + " ('" + item.stockUnit() + "')");
            }
            return new RecipeLine(lineNumber, item.id(), null, line.grossQuantity(), unitId, yieldPct);
        }

        Recipe sub = Optional.ofNullable(model.recipes().get(line.subRecipeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Escandallo no encontrado con ID: " + line.subRecipeId()));
        if (sub.getType() != RecipeType.PREPARATION) {
            throw new BadRequestException("'" + sub.getName() + "' es un plato: solo las elaboraciones se usan como ingrediente");
        }
        if (Objects.equals(sub.getId(), recipe.getId())) {
            throw new BadRequestException("Un escandallo no puede usarse a sí mismo");
        }
        String subUnit = model.unitCode(sub.getYieldUnitId());
        if (!model.outputConverterOf(sub).canConvert(line.unit(), subUnit)) {
            throw new BadRequestException("'" + line.unit() + "' no se puede pasar a la unidad de rendimiento de '"
                    + sub.getName() + "' ('" + subUnit + "')");
        }
        return new RecipeLine(lineNumber, null, recipeRepository.getReferenceById(sub.getId()), line.grossQuantity(),
                unitId, yieldPct);
    }

    private void rejectCycles(Recipe recipe, List<RecipeLine> newLines, RecipeModelLoader.Model model) {
        Map<Long, Set<Long>> edges = new HashMap<>();
        model.recipes().values().forEach(r -> edges.put(r.getId(), r.getLines().stream()
                .filter(l -> l.getSubRecipe() != null)
                .map(l -> l.getSubRecipe().getId())
                .collect(Collectors.toSet())));
        edges.put(recipe.getId(), newLines.stream()
                .filter(l -> l.getSubRecipe() != null)
                .map(l -> l.getSubRecipe().getId())
                .collect(Collectors.toSet()));
        RecipeGraph.findCycle(edges, recipe.getId()).ifPresent(cycle -> {
            String path = cycle.stream()
                    .map(id -> id.equals(recipe.getId()) ? recipe.getName() : model.recipes().get(id).getName())
                    .collect(Collectors.joining(" -> "));
            throw new BadRequestException("Los sub-escandallos forman un ciclo: " + path);
        });
    }

    private void recordCost(Long recipeId, Reason reason) {
        RecipeModelLoader.Model model = modelLoader.load(true);
        recordIfChanged(recipeId, model.costing().cost(recipeId), reason);
    }

    private boolean recordIfChanged(Long recipeId, RecipeCost cost, Reason reason) {
        Optional<RecipeCostHistory> last = historyRepository.findFirstByRecipeIdOrderByRecordedAtDescIdDesc(recipeId);
        boolean changed = last.isEmpty()
                || last.get().getTotalCost().compareTo(cost.totalCost()) != 0
                || last.get().getComplete() != cost.complete();
        if (changed) {
            historyRepository.save(new RecipeCostHistory(recipeId, cost.totalCost(), cost.costPerOutputUnit(),
                    cost.complete(), reason));
        }
        return changed;
    }

    private void collectWithParents(Long recipeId, RecipeModelLoader.Model model, Set<Long> collected) {
        if (!collected.add(recipeId)) {
            return;
        }
        model.recipes().values().stream()
                .filter(r -> r.getLines().stream()
                        .anyMatch(l -> l.getSubRecipe() != null && recipeId.equals(l.getSubRecipe().getId())))
                .forEach(parent -> collectWithParents(parent.getId(), model, collected));
    }

    private Long unitId(String code, RecipeModelLoader.Model model) {
        Long id = model.unitIds().get(code);
        if (id == null) {
            throw new BadRequestException("Unidad de medida desconocida: '" + code + "'");
        }
        return id;
    }

    private Recipe findRecipe(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escandallo no encontrado con ID: " + id));
    }

    private static boolean showsCosts(CurrentUser requester) {
        return requester.role() != UserRole.STAFF;
    }

    private RecipeDTO toDTO(Recipe recipe, RecipeModelLoader.Model model, Map<Long, ProductDTO> productCache,
                            CurrentUser requester) {
        boolean showCosts = showsCosts(requester);
        RecipeCost cost = model.costing().cost(recipe.getId());
        ProductDTO product = recipe.getProductId() == null ? null
                : productCache.computeIfAbsent(recipe.getProductId(), productService::getProductById);
        StockItemDTO output = recipe.getStockItemId() == null ? null : model.items().get(recipe.getStockItemId());
        BigDecimal target = recipe.getTargetFoodCostPct() != null ? recipe.getTargetFoodCostPct() : defaultTargetFoodCostPct;

        List<RecipeDTOs.Line> lines = new ArrayList<>();
        for (int i = 0; i < recipe.getLines().size(); i++) {
            RecipeLine line = recipe.getLines().get(i);
            RecipeCosting.LineCost lineCost = cost.lines().get(i);
            StockItemDTO item = line.getStockItemId() == null ? null : model.items().get(line.getStockItemId());
            lines.add(new RecipeDTOs.Line(line.getLineNumber(), line.getStockItemId(),
                    item != null ? item.code() : null, item != null ? item.name() : null,
                    line.getSubRecipe() != null ? line.getSubRecipe().getId() : null,
                    line.getSubRecipe() != null ? line.getSubRecipe().getName() : null,
                    line.getGrossQuantity(), model.unitCode(line.getUnitId()), line.getYieldPct(),
                    lineCost.netQuantity(),
                    showCosts ? lineCost.unitCost() : null,
                    showCosts ? lineCost.cost() : null));
        }

        boolean dish = recipe.getType() == RecipeType.DISH;
        boolean priced = showCosts && dish && cost.complete() && product != null;
        BigDecimal costPerPortion = dish ? cost.costPerOutputUnit() : null;
        return new RecipeDTO(recipe.getId(), recipe.getName(), recipe.getType(), recipe.getProductId(),
                product != null ? product.getName() : null, product != null ? product.getPrice() : null,
                product != null ? product.getVatRate() : null,
                recipe.getStockItemId(), output != null ? output.code() : null,
                recipe.getPortions(), recipe.getYieldQuantity(), model.unitCode(recipe.getYieldUnitId()),
                target, recipe.getNotes(), recipe.getActive(), lines, cost.allergens(),
                showCosts ? cost.totalCost() : null,
                showCosts ? costPerPortion : null,
                showCosts && !dish ? cost.costPerOutputUnit() : null,
                showCosts ? cost.complete() : null,
                showCosts ? cost.missingItemIds().stream().map(id -> model.items().get(id).code()).toList() : null,
                priced ? MenuPricing.foodCostPct(costPerPortion, product.getPrice(), product.getVatRate()) : null,
                priced ? MenuPricing.grossMargin(costPerPortion, product.getPrice(), product.getVatRate()) : null,
                priced ? MenuPricing.suggestedPrice(costPerPortion, target, product.getVatRate()) : null);
    }
}
