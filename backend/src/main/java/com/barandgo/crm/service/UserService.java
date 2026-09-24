package com.barandgo.crm.service;

import com.barandgo.crm.dto.user.UserRequestDTO;
import com.barandgo.crm.dto.user.UserResponseDTO;
import com.barandgo.crm.dto.user.UserUpdateDTO;
import java.util.List;

public interface UserService {
    
    UserResponseDTO createUser(UserRequestDTO userRequestDTO);
    
    UserResponseDTO getUserById(Long id);
    
    List<UserResponseDTO> getAllUsers();
    
    UserResponseDTO updateUser(Long id, UserUpdateDTO userUpdateDTO);
    
    void deleteUser(Long id);
    
    UserResponseDTO getUserByEmail(String email);
}