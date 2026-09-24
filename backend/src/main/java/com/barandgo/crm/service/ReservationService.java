package com.barandgo.crm.service;

import com.barandgo.crm.dto.reservation.ReservationRequestDTO;
import com.barandgo.crm.dto.reservation.ReservationResponseDTO;
import com.barandgo.crm.dto.reservation.ReservationUpdateDTO;
import com.barandgo.crm.enums.ReservationStatus;
import com.barandgo.crm.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;

public interface ReservationService {
    
    ReservationResponseDTO createReservation(ReservationRequestDTO reservationRequestDTO);
    
    ReservationResponseDTO getReservationById(Long id);
 
    List<ReservationResponseDTO> getAllReservations();
    
    List<ReservationResponseDTO> getReservationsBetween(LocalDate from, LocalDate to);
    
    List<ReservationResponseDTO> getUpcomingReservations(ReservationStatus status);
    
    List<ReservationResponseDTO> getReservationsForUser(Long userId);
    
    ReservationResponseDTO updateReservation(Long id, ReservationUpdateDTO reservationUpdateDTO);
    
    ReservationResponseDTO cancelReservation(Long id, CurrentUser requester);
    
    void deleteReservation(Long id);
    
    List<ReservationResponseDTO> getReservationsByDate(LocalDate date);
    
    List<ReservationResponseDTO> getReservationsByStatus(ReservationStatus status);
}
