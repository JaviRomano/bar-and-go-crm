package com.barandgo.crm.masterdata;

import java.math.BigDecimal;
import java.util.List;


public interface UnitOfMeasureService {

    List<UnitOfMeasureDTO> getUnits();

    UnitOfMeasureDTO createUnit(UnitOfMeasureDTO unitDTO);

    List<UnitConversionDTO> getConversions();

    UnitConversionDTO createConversion(UnitConversionDTO conversionDTO);

    void deleteConversion(Long id);

    ConversionResultDTO convert(BigDecimal quantity, String fromUnit, String toUnit);

    /** Conversor con las reglas genéricas; los artículos le añaden las suyas con withItemRules. */
    UnitConverter genericConverter();
}
