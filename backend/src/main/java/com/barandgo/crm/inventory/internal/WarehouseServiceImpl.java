package com.barandgo.crm.inventory.internal;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.inventory.WarehouseDTO;
import com.barandgo.crm.inventory.WarehouseService;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;

@Service
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseServiceImpl.class);

    private final WarehouseRepository warehouseRepository;
    private final SiteRepository siteRepository;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository, SiteRepository siteRepository) {
        this.warehouseRepository = warehouseRepository;
        this.siteRepository = siteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDTO> getWarehouses(boolean includeInactive) {
        return warehouseRepository.findAllByOrderByNameAsc().stream()
                .filter(warehouse -> includeInactive || warehouse.getActive())
                .map(WarehouseServiceImpl::toDTO)
                .toList();
    }

    @Override
    public WarehouseDTO createWarehouse(WarehouseDTO dto) {
        Long siteId = resolveSite(dto.siteId());
        String name = dto.name().trim();
        if (warehouseRepository.existsBySiteIdAndNameIgnoreCase(siteId, name)) {
            throw new ConflictException("Ya existe un almacén llamado '" + name + "' en el local");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setSiteId(siteId);
        warehouse.setName(name);
        warehouse.setType(dto.type());
        warehouse.setActive(dto.active() != null ? dto.active() : true);
        applySalesFlag(warehouse, dto.defaultForSales());

        Warehouse saved = warehouseRepository.save(warehouse);
        logger.info("Almacén '{}' creado", saved.getName());
        return toDTO(saved);
    }

    // El local no cambia: mover un almacén de local alteraría la valoración de los dos
    @Override
    public WarehouseDTO updateWarehouse(Long id, WarehouseDTO dto) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con ID: " + id));
        String name = dto.name().trim();
        if (warehouseRepository.existsBySiteIdAndNameIgnoreCaseAndIdNot(warehouse.getSiteId(), name, id)) {
            throw new ConflictException("Ya existe un almacén llamado '" + name + "' en el local");
        }

        warehouse.setName(name);
        warehouse.setType(dto.type());
        if (dto.active() != null) {
            warehouse.setActive(dto.active());
        }
        applySalesFlag(warehouse, dto.defaultForSales());

        logger.info("Almacén {} actualizado", id);
        return toDTO(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WarehouseDTO> getSalesWarehouse() {
        return warehouseRepository.findAll().stream()
                .filter(w -> Boolean.TRUE.equals(w.getDefaultForSales()) && Boolean.TRUE.equals(w.getActive()))
                .findFirst()
                .map(WarehouseServiceImpl::toDTO);
    }

    // Uno por local: marcar este desmarca el anterior (lo exige el índice único parcial)
    private void applySalesFlag(Warehouse warehouse, Boolean defaultForSales) {
        if (defaultForSales == null) {
            return;
        }
        if (defaultForSales) {
            warehouseRepository.findAll().stream()
                    .filter(w -> w.getSiteId().equals(warehouse.getSiteId()) && !w.getId().equals(warehouse.getId())
                            && Boolean.TRUE.equals(w.getDefaultForSales()))
                    .forEach(w -> {
                        w.setDefaultForSales(false);
                        warehouseRepository.saveAndFlush(w);
                    });
        }
        warehouse.setDefaultForSales(defaultForSales);
    }

    private Long resolveSite(Long siteId) {
        if (siteId == null) {
            return siteRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new BadRequestException("No hay ningún local dado de alta"))
                    .getId();
        }
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + siteId))
                .getId();
    }

    static WarehouseDTO toDTO(Warehouse warehouse) {
        return new WarehouseDTO(warehouse.getId(), warehouse.getSiteId(), warehouse.getName(), warehouse.getType(),
                warehouse.getActive(), warehouse.getDefaultForSales());
    }
}
