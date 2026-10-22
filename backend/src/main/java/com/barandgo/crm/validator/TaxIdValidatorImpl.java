package com.barandgo.crm.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TaxIdValidatorImpl implements ConstraintValidator<TaxIdValidator, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Dejar que @NotBlank lo maneje
        }
        return TaxIdRules.isValid(TaxIdRules.normalize(value));
    }
}
