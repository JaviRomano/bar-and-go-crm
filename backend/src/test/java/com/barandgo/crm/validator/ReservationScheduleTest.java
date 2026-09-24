package com.barandgo.crm.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReservationScheduleTest {

    private static final LocalDate NEXT_WEDNESDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
    private static final LocalDate NEXT_TUESDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));

    @ParameterizedTest
    @ValueSource(strings = {"12:00", "14:30", "16:00", "20:00", "23:00"})
    void acceptsTimesInsideServiceHours(String time) {
        assertThat(ReservationSchedule.check(NEXT_WEDNESDAY, LocalTime.parse(time))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11:59", "16:01", "19:59", "23:01", "09:00"})
    void rejectsTimesOutsideServiceHours(String time) {
        assertThat(ReservationSchedule.check(NEXT_WEDNESDAY, LocalTime.parse(time)))
                .hasValueSatisfying(violation -> assertThat(violation.field()).isEqualTo("time"));
    }

    @Test
    void rejectsTuesdayEvenInsideServiceHours() {
        assertThat(ReservationSchedule.check(NEXT_TUESDAY, LocalTime.of(13, 0)))
                .hasValueSatisfying(violation -> assertThat(violation.field()).isEqualTo("date"));
    }
}
