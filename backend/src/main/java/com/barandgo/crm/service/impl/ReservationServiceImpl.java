package com.barandgo.crm.service.impl;

import com.barandgo.crm.dto.reservation.ReservationRequestDTO;
import com.barandgo.crm.dto.reservation.ReservationResponseDTO;
import com.barandgo.crm.dto.reservation.ReservationUpdateDTO;
import com.barandgo.crm.entity.Reservation;
import com.barandgo.crm.entity.User;
import com.barandgo.crm.enums.ReservationStatus;
import com.barandgo.crm.exception.BadRequestException;
import com.barandgo.crm.exception.ForbiddenException;
import com.barandgo.crm.exception.ResourceNotFoundException;
import com.barandgo.crm.repository.ReservationRepository;
import com.barandgo.crm.repository.UserRepository;
import com.barandgo.crm.security.CurrentUser;
import com.barandgo.crm.service.ReservationService;
import com.barandgo.crm.validator.ReservationSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    public ReservationServiceImpl(ReservationRepository reservationRepository, UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ReservationResponseDTO createReservation(ReservationRequestDTO reservationRequestDTO) {
        if (reservationRequestDTO.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("No se pueden crear reservas con fecha pasada");
        }

        if (reservationRequestDTO.getPax() < 1) {
            throw new BadRequestException("El número de personas debe ser mayor a 0");
        }

        Reservation reservation = new Reservation();
        reservation.setDate(reservationRequestDTO.getDate());
        reservation.setTime(reservationRequestDTO.getTime());
        reservation.setPax(reservationRequestDTO.getPax());
        reservation.setUserName(reservationRequestDTO.getUserName());
        reservation.setUserPhone(reservationRequestDTO.getUserPhone());
        reservation.setComments(reservationRequestDTO.getComments());

        Reservation savedReservation = reservationRepository.save(reservation);
        logger.info("Reserva {} creada para el {}", savedReservation.getId(), savedReservation.getDate());

        return convertToResponseDTO(savedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponseDTO getReservationById(Long id) {
        return convertToResponseDTO(findReservation(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getAllReservations() {
        return toResponseList(reservationRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getReservationsBetween(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("La fecha 'from' no puede ser posterior a 'to'");
        }
        return toResponseList(reservationRepository.findReservationsBetweenDates(from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getUpcomingReservations(ReservationStatus status) {
        return toResponseList(reservationRepository.findFutureReservationsByStatus(LocalDate.now(), status));
    }

    /**
     * Las reservas no tienen relación con User: se asocian por el teléfono de contacto.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getReservationsForUser(Long userId) {
        return toResponseList(reservationRepository.findByUserPhone(findUser(userId).getPhone()));
    }

    @Override
    public ReservationResponseDTO updateReservation(Long id, ReservationUpdateDTO reservationUpdateDTO) {
        Reservation reservation = findReservation(id);

        if (reservationUpdateDTO.getDate() != null && reservationUpdateDTO.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("No se puede actualizar a una fecha pasada");
        }

        if (reservationUpdateDTO.getPax() != null && reservationUpdateDTO.getPax() < 1) {
            throw new BadRequestException("El número de personas debe ser mayor a 0");
        }

        // El horario se valida sobre el resultado final, combinando lo recibido con lo existente
        LocalDate newDate = reservationUpdateDTO.getDate() != null ? reservationUpdateDTO.getDate() : reservation.getDate();
        LocalTime newTime = reservationUpdateDTO.getTime() != null ? reservationUpdateDTO.getTime() : reservation.getTime();
        ReservationSchedule.check(newDate, newTime).ifPresent(violation -> {
            throw new BadRequestException(violation.message());
        });

        reservation.setDate(newDate);
        reservation.setTime(newTime);

        if (reservationUpdateDTO.getPax() != null) {
            reservation.setPax(reservationUpdateDTO.getPax());
        }

        if (reservationUpdateDTO.getComments() != null) {
            reservation.setComments(reservationUpdateDTO.getComments());
        }

        if (reservationUpdateDTO.getStatus() != null) {
            reservation.setStatus(reservationUpdateDTO.getStatus());
        }

        Reservation updatedReservation = reservationRepository.save(reservation);
        logger.info("Reserva {} actualizada", updatedReservation.getId());

        return convertToResponseDTO(updatedReservation);
    }

    @Override
    public ReservationResponseDTO cancelReservation(Long id, CurrentUser requester) {
        Reservation reservation = findReservation(id);

        if (!requester.isAdmin()
                && !reservation.getUserPhone().equals(findUser(requester.id()).getPhone())) {
            throw new ForbiddenException("No tienes acceso a esta reserva");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation cancelledReservation = reservationRepository.save(reservation);

        logger.info("Reserva {} cancelada", cancelledReservation.getId());

        return convertToResponseDTO(cancelledReservation);
    }

    @Override
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reserva no encontrada con ID: " + id);
        }

        reservationRepository.deleteById(id);
        logger.info("Reserva {} eliminada", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getReservationsByDate(LocalDate date) {
        return toResponseList(reservationRepository.findByDate(date));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getReservationsByStatus(ReservationStatus status) {
        return toResponseList(reservationRepository.findByStatus(status));
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
    }

    private List<ReservationResponseDTO> toResponseList(List<Reservation> reservations) {
        return reservations.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    private ReservationResponseDTO convertToResponseDTO(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setDate(reservation.getDate());
        dto.setTime(reservation.getTime());
        dto.setPax(reservation.getPax());
        dto.setUserName(reservation.getUserName());
        dto.setUserPhone(reservation.getUserPhone());
        dto.setComments(reservation.getComments());
        dto.setStatus(reservation.getStatus());
        return dto;
    }
}
