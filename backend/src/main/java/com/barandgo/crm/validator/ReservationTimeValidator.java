package com.barandgo.crm.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = ReservationTimeValidatorImpl.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReservationTimeValidator {
    
    String message() default "Horario de reserva no válido. Horarios permitidos: 12:00-16:00 y 20:00-23:00. Cerrado los martes.";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}