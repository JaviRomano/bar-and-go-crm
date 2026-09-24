package com.barandgo.crm.dto.reservation;

import com.barandgo.crm.enums.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationResponseDTO {
    
    private Long id;
    private LocalDate date;
    private LocalTime time;
    private Integer pax;
    private String userName;
    private String userPhone;
    private String comments;
    private ReservationStatus status;
    
    public ReservationResponseDTO() {
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