package com.barandgo.crm.masterdata.internal;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.masterdata.ConversionResultDTO;
import com.barandgo.crm.masterdata.UnitConversionDTO;
import com.barandgo.crm.masterdata.UnitOfMeasureDTO;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.masterdata.UnitConverter;

@Service
@Transactional
public class UnitOfMeasureServiceImpl implements UnitOfMeasureService {

    private static final Logger logger = LoggerFactory.getLogger(UnitOfMeasureServiceImpl.class);

    private final UnitOfMeasureRepository unitRepository;
    private final UnitConversionRepository conversionRepository;

    public UnitOfMeasureServiceImpl(UnitOfMeasureRepository unitRepository,
                                    UnitConversionRepository conversionRepository) {
        this.unitRepository = unitRepository;
        this.conversionRepository = conversionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitOfMeasureDTO> getUnits() {
        return unitRepository.findAllByOrderByDimensionAscCodeAsc().stream().map(UnitOfMeasureDTO::from).toList();
    }

    @Override
    public UnitOfMeasureDTO createUnit(UnitOfMeasureDTO unitDTO) {
        if (unitRepository.existsByCode(unitDTO.code())) {
            throw new ConflictException("Ya existe una unidad con el código '" + unitDTO.code() + "'");
        }

        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setCode(unitDTO.code());
        unit.setName(unitDTO.name().trim());
        unit.setDimension(unitDTO.dimension());

        UnitOfMeasure saved = unitRepository.save(unit);
        logger.info("Unidad de medida '{}' creada", saved.getCode());
        return UnitOfMeasureDTO.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitConversionDTO> getConversions() {
        return conversionRepository.findAll().stream().map(UnitConversionDTO::from).toList();
    }

    @Override
    public UnitConversionDTO createConversion(UnitConversionDTO conversionDTO) {
        UnitOfMeasure from = findUnit(conversionDTO.fromUnit());
        UnitOfMeasure to = findUnit(conversionDTO.toUnit());

        if (from.getId().equals(to.getId())) {
            throw new BadRequestException("Una conversión necesita dos unidades distintas");
        }
        // Entre magnitudes distintas (ud -> l) la equivalencia depende del artículo: no es genérica
        if (from.getDimension() != to.getDimension()) {
            throw new BadRequestException("Solo se admiten conversiones genéricas dentro de la misma magnitud ("
                    + from.getDimension() + " y " + to.getDimension() + "). Las demás se definen por artículo");
        }
        // Si ya se puede convertir, la regla nueva sobraría o contradiría a las existentes
        UnitConverter converter = genericConverter();
        if (converter.canConvert(from.getCode(), to.getCode())) {
            throw new ConflictException("Ya existe una conversión entre '" + from.getCode() + "' y '" + to.getCode()
                    + "': 1 " + from.getCode() + " = " + converter.factor(from.getCode(), to.getCode()).toPlainString()
                    + " " + to.getCode());
        }

        UnitConversion conversion = new UnitConversion();
        conversion.setFromUnit(from);
        conversion.setToUnit(to);
        conversion.setFactor(conversionDTO.factor());

        UnitConversion saved = conversionRepository.save(conversion);
        logger.info("Conversión creada: 1 {} = {} {}", from.getCode(), saved.getFactor(), to.getCode());
        return UnitConversionDTO.from(saved);
    }

    @Override
    public void deleteConversion(Long id) {
        if (!conversionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conversión no encontrada con ID: " + id);
        }
        conversionRepository.deleteById(id);
        logger.info("Conversión {} eliminada", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversionResultDTO convert(BigDecimal quantity, String fromUnit, String toUnit) {
        findUnit(fromUnit);
        findUnit(toUnit);
        BigDecimal result = genericConverter().convert(quantity, fromUnit, toUnit);
        return new ConversionResultDTO(quantity, fromUnit, toUnit, result);
    }

    @Override
    @Transactional(readOnly = true)
    public UnitConverter genericConverter() {
        List<UnitConverter.Rule> rules = conversionRepository.findAll().stream()
                .map(c -> new UnitConverter.Rule(c.getFromUnit().getCode(), c.getToUnit().getCode(), c.getFactor()))
                .toList();
        return new UnitConverter(rules);
    }

    private UnitOfMeasure findUnit(String code) {
        return unitRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada: " + code));
    }
}
