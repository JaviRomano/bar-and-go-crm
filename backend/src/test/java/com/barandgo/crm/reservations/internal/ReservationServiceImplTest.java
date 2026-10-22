package com.barandgo.crm.reservations.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barandgo.crm.reservations.ReservationUpdateDTO;
import com.barandgo.crm.identity.User;
import com.barandgo.crm.reservations.ReservationStatus;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ForbiddenException;
import com.barandgo.crm.identity.UserDirectory;
import com.barandgo.crm.identity.CurrentUser;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final LocalDate NEXT_WEDNESDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
    private static final LocalDate NEXT_TUESDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserDirectory userDirectory;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void updateCannotMoveReservationToClosingDay() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation(1L, "+34600000001")));

        ReservationUpdateDTO update = new ReservationUpdateDTO();
        update.setDate(NEXT_TUESDAY);

        assertThatThrownBy(() -> reservationService.updateReservation(1L, update))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("martes");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateValidatesNewTimeAgainstExistingDate() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation(1L, "+34600000001")));

        ReservationUpdateDTO update = new ReservationUpdateDTO();
        update.setTime(LocalTime.of(18, 0));

        assertThatThrownBy(() -> reservationService.updateReservation(1L, update))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void customerCanCancelOwnReservation() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation(1L, "+34600000001")));
        when(userDirectory.getUser(5L)).thenReturn(user(5L, "+34600000001"));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrentUser owner = new CurrentUser(5L, "ana@example.com", UserRole.CUSTOMER);

        assertThat(reservationService.cancelReservation(1L, owner).getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void customerCannotCancelSomeoneElsesReservation() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation(1L, "+34600000001")));
        when(userDirectory.getUser(6L)).thenReturn(user(6L, "+34699999999"));

        CurrentUser stranger = new CurrentUser(6L, "otro@example.com", UserRole.CUSTOMER);

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, stranger))
                .isInstanceOf(ForbiddenException.class);
        verify(reservationRepository, never()).save(any());
    }

    private static Reservation reservation(Long id, String phone) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setDate(NEXT_WEDNESDAY);
        reservation.setTime(LocalTime.of(21, 0));
        reservation.setPax(2);
        reservation.setUserName("Ana");
        reservation.setUserPhone(phone);
        reservation.setStatus(ReservationStatus.PENDING);
        return reservation;
    }

    private static User user(Long id, String phone) {
        User user = new User();
        user.setId(id);
        user.setPhone(phone);
        user.setRole(UserRole.CUSTOMER);
        return user;
    }
}
