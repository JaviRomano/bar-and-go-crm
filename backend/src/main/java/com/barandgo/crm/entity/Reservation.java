package com.barandgo.crm.entity;

import com.barandgo.crm.enums.ReservationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    private LocalTime time;
    
    @Column(nullable = false)
    private Integer pax;
    
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;
    
    @Column(name = "user_phone", nullable = false, length = 20)
    private String userPhone;
    
    @Column(length = 500)
    private String comments;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;
    
    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ReservationStatus.PENDING;
        }
    }

    // Constructor vacío
    public Reservation() {
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public LocalTime getTime() {
        return time;
    }
    
    public void setTime(LocalTime time) {
        this.time = time;
    }
    
    public Integer getPax() {
        return pax;
    }
    
    public void setPax(Integer pax) {
        this.pax = pax;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserPhone() {
        return userPhone;
    }
    
    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }
    
    public String getComments() {
        return comments;
    }
    
    public void setComments(String comments) {
        this.comments = comments;
    }
    
    public ReservationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}