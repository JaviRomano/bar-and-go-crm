package com.barandgo.crm.validator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Horario de reservas del local. Se usa tanto en la validación de alta
 * (ReservationTimeValidatorImpl) como al modificar una reserva existente.
 */
public final class ReservationSchedule {

    private static final DayOfWeek CLOSING_DAY = DayOfWeek.TUESDAY;
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(16, 0);
    private static final LocalTime DINNER_START = LocalTime.of(20, 0);
    private static final LocalTime DINNER_END = LocalTime.of(23, 0);

    public record Violation(String field, String message) {
    }

    private ReservationSchedule() {
    }

    public static Optional<Violation> check(LocalDate date, LocalTime time) {
        if (date.getDayOfWeek() == CLOSING_DAY) {
            return Optional.of(new Violation("date", "Los martes el bar está cerrado"));
        }

        boolean isLunchTime = !time.isBefore(LUNCH_START) && !time.isAfter(LUNCH_END);
        boolean isDinnerTime = !time.isBefore(DINNER_START) && !time.isAfter(DINNER_END);

        if (!isLunchTime && !isDinnerTime) {
            return Optional.of(new Violation("time",
                    "Horarios de reserva: 12:00-16:00 (almuerzo) y 20:00-23:00 (cena)"));
        }

        return Optional.empty();
    }
}
