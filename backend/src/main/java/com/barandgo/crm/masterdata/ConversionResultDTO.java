package com.barandgo.crm.masterdata;

import java.math.BigDecimal;

public record ConversionResultDTO(BigDecimal quantity, String fromUnit, String toUnit, BigDecimal result) {
}
