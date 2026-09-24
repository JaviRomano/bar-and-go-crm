package com.barandgo.crm.dto.reservation;

import com.barandgo.crm.enums.ReservationStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationUpdateDTO {
    
    @Future(message = "La fecha debe ser futura")
    private LocalDate date;
    
    private LocalTime time;
    
    @Min(value = 1, message = "Debe haber al menos 1 persona")
    @Max(value = 50, message = "No se permiten reservas para más de 50 personas")
    private Integer pax;
    
    @Size(max = 500, message = "Los comentarios no pueden exceder 500 caracteres")
    private String comments;
    
    private ReservationStatus status;
    
    public ReservationUpdateDTO() {
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