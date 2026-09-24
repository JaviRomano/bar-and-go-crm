package com.barandgo.crm.dto.order;

import jakarta.validation.Valid;
import com.barandgo.crm.enums.PaymentMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Pedido entrante. El usuario no viaja en el cuerpo: se obtiene del token.
 */
public class OrderRequestDTO {

    @NotNull(message = "La hora de recogida es obligatoria")
    @Future(message = "La hora de recogida debe ser futura")
    private LocalDateTime timeTakeAway;

    @NotNull(message = "El método de pago es obligatorio")
    private PaymentMethod paymentMethod;
    
    @NotEmpty(message = "El pedido debe contener al menos un producto")
    private List<@Valid OrderItemRequestDTO> items = new ArrayList<>();

    public OrderRequestDTO() {
    }

    public LocalDateTime getTimeTakeAway() {
        return timeTakeAway;
    }

    public void setTimeTakeAway(LocalDateTime timeTakeAway) {
        this.timeTakeAway = timeTakeAway;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public List<OrderItemRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequestDTO> items) {
        this.items = items;
    }
}
