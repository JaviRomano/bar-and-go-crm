package com.barandgo.crm.service;

import com.barandgo.crm.dto.user.UserRequestDTO;
import com.barandgo.crm.dto.user.UserResponseDTO;
import com.barandgo.crm.dto.user.UserUpdateDTO;
import com.barandgo.crm.security.CurrentUser;
import java.util.List;

/**
 * Gestión de usuarios. Las operaciones con requester aplican las reglas de rol:
 * ADMIN gestiona a cualquiera; MANAGER solo a STAFF y CUSTOMER.
 */
public interface UserService {

    /** Alta pública de cliente (registro). Ignora el rol y las etiquetas recibidos. */
    UserResponseDTO registerCustomer(UserRequestDTO userRequestDTO);

    UserResponseDTO createUser(UserRequestDTO userRequestDTO, CurrentUser requester);

    UserResponseDTO getUserById(Long id);

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO updateUser(Long id, UserUpdateDTO userUpdateDTO, CurrentUser requester);

    void deleteUser(Long id, CurrentUser requester);

    UserResponseDTO getUserByEmail(String email);
}
