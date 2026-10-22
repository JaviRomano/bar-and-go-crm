package com.barandgo.crm.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * CIF/NIF/NIE español con dígito de control válido, o NIF-IVA de otro país de la UE.
 * Un valor nulo se considera válido: la obligatoriedad la marca @NotBlank.
 */
@Documented
@Constraint(validatedBy = TaxIdValidatorImpl.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TaxIdValidator {

    String message() default "El CIF/NIF no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
