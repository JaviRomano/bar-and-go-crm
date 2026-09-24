package com.barandgo.crm.controller;

import com.barandgo.crm.dto.reservation.ReservationRequestDTO;
import com.barandgo.crm.dto.reservation.ReservationResponseDTO;
import com.barandgo.crm.dto.reservation.ReservationUpdateDTO;
import com.barandgo.crm.enums.ReservationStatus;
import com.barandgo.crm.exception.BadRequestException;
import com.barandgo.crm.security.CurrentUser;
import com.barandgo.crm.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponseDTO> createReservation(
            @Valid @RequestBody ReservationRequestDTO reservationRequestDTO) {
        ReservationResponseDTO createdReservation = reservationService.createReservation(reservationRequestDTO);
        return new ResponseEntity<>(createdReservation, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDTO> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    /**
     * Todas las reservas, o las de un intervalo de fechas si se indican from y to (yyyy-MM-dd).
     */
    @GetMapping
    public ResponseEntity<List<ReservationResponseDTO>> getReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null && to == null) {
            return ResponseEntity.ok(reservationService.getAllReservations());
        }
        if (from == null || to == null) {
            throw new BadRequestException("Para filtrar por fechas hay que indicar 'from' y 'to'");
        }
        return ResponseEntity.ok(reservationService.getReservationsBetween(from, to));
    }

    /**
     * Reservas del usuario autenticado (asociadas por su teléfono).
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponseDTO>> getMyReservations(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reservationService.getReservationsForUser(CurrentUser.from(jwt).id()));
    }

    /**
     * Reservas desde hoy en un estado dado, por defecto las confirmadas.
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<ReservationResponseDTO>> getUpcomingReservations(
            @RequestParam(defaultValue = "CONFIRMED") ReservationStatus status) {
        return ResponseEntity.ok(reservationService.getUpcomingReservations(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponseDTO> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateDTO reservationUpdateDTO) {
        return ResponseEntity.ok(reservationService.updateReservation(id, reservationUpdateDTO));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponseDTO> cancelReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, CurrentUser.from(jwt)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<ReservationResponseDTO>> getReservationsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reservationService.getReservationsByDate(date));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReservationResponseDTO>> getReservationsByStatus(
            @PathVariable ReservationStatus status) {
        return ResponseEntity.ok(reservationService.getReservationsByStatus(status));
    }
}
