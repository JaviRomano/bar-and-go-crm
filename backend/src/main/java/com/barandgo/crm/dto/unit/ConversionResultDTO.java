package com.barandgo.crm.dto.unit;

import java.math.BigDecimal;

public record ConversionResultDTO(BigDecimal quantity, String fromUnit, String toUnit, BigDecimal result) {
}
