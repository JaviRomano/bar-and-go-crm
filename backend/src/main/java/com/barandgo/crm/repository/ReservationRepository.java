package com.barandgo.crm.repository;

import com.barandgo.crm.entity.Reservation;
import com.barandgo.crm.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByDate(LocalDate date);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByUserPhone(String userPhone);

    @Query("SELECT r FROM Reservation r WHERE r.date BETWEEN :startDate AND :endDate ORDER BY r.date, r.time")
    List<Reservation> findReservationsBetweenDates(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
   
    @Query("SELECT r FROM Reservation r WHERE r.date >= :date AND r.status = :status ORDER BY r.date, r.time")
    List<Reservation> findFutureReservationsByStatus(
        @Param("date") LocalDate date, 
        @Param("status") ReservationStatus status
    );
}