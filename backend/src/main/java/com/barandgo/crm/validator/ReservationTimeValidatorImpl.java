package com.barandgo.crm.validator;

import com.barandgo.crm.dto.reservation.ReservationRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReservationTimeValidatorImpl implements ConstraintValidator<ReservationTimeValidator, ReservationRequestDTO> {

    @Override
    public boolean isValid(ReservationRequestDTO dto, ConstraintValidatorContext context) {

        if (dto.getDate() == null || dto.getTime() == null) {
            return true; // Dejar que @NotNull lo maneje
        }

        return ReservationSchedule.check(dto.getDate(), dto.getTime())
                .map(violation -> {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(violation.message())
                            .addPropertyNode(violation.field())
                            .addConstraintViolation();
                    return false;
                })
                .orElse(true);
    }
}
