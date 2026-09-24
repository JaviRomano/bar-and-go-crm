package com.barandgo.crm.dto.reservation;

import java.time.LocalDate;
import java.time.LocalTime;

import com.barandgo.crm.validator.ReservationTimeValidator;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


@ReservationTimeValidator
public class ReservationRequestDTO {
    
    @NotNull(message = "La fecha es obligatoria")
    @Future(message = "La fecha debe ser futura")
    private LocalDate date;
    
    @NotNull(message = "La hora es obligatoria")
    private LocalTime time;
    
    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 persona")
    @Max(value = 50, message = "No se permiten reservas para más de 50 personas")
    private Integer pax;
    
    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String userName;
    
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[+]?[0-9]{9,20}$", message = "El teléfono debe ser válido")
    private String userPhone;
    
    @Size(max = 500, message = "Los comentarios no pueden exceder 500 caracteres")
    private String comments;
    
    public ReservationRequestDTO() {
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
}