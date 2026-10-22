package com.barandgo.crm.identity;

import com.barandgo.crm.shared.ResourceNotFoundException;

/**
 * Consulta de usuarios para otros módulos. Pedidos necesita la entidad para su relación JPA;
 * reservas, el teléfono con el que se asocian las reservas de un cliente.
 */
public interface UserDirectory {

    /**
     * @throws ResourceNotFoundException si no existe
     */
    User getUser(Long id);

    boolean exists(Long id);
}
