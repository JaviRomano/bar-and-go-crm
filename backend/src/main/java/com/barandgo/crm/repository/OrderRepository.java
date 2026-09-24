package com.barandgo.crm.repository;

import com.barandgo.crm.entity.Order;
import com.barandgo.crm.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
 
    List<Order> findByUserId(Long userId);
    
    List<Order> findByStatus(OrderStatus status);
    
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.dateOrder BETWEEN :startDate AND :endDate ORDER BY o.dateOrder DESC")
    List<Order> findOrdersBetweenDates(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT o FROM Order o WHERE o.timeTakeAway > :currentDateTime ORDER BY o.timeTakeAway")
    List<Order> findFutureOrders(@Param("currentDateTime") LocalDateTime currentDateTime);
}