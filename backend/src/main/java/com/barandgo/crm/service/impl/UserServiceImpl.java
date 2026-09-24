package com.barandgo.crm.service.impl;

import com.barandgo.crm.dto.user.UserRequestDTO;
import com.barandgo.crm.dto.user.UserResponseDTO;
import com.barandgo.crm.dto.user.UserUpdateDTO;
import com.barandgo.crm.entity.User;
import com.barandgo.crm.exception.BadRequestException;
import com.barandgo.crm.exception.ResourceNotFoundException;
import com.barandgo.crm.repository.UserRepository;
import com.barandgo.crm.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }

        // Convertir DTO a entidad
        User user = new User();
        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());

        String encryptedPassword = passwordEncoder.encode(userRequestDTO.getPassword());
        user.setPassword(encryptedPassword);

        user.setPhone(userRequestDTO.getPhone());
        user.setRole(userRequestDTO.getRole());

        User savedUser = userRepository.save(user);
        logger.info("Usuario {} creado con rol {}", savedUser.getId(), savedUser.getRole());

        return UserResponseDTO.from(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        return UserResponseDTO.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (userUpdateDTO.getEmail() != null && !userUpdateDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(userUpdateDTO.getEmail(), id)) {
                throw new BadRequestException("Ya existe otro usuario con ese email");
            }
            user.setEmail(userUpdateDTO.getEmail());
        }

        // Actualizar solo los campos no nulos
        if (userUpdateDTO.getName() != null) {
            user.setName(userUpdateDTO.getName());
        }

        if (userUpdateDTO.getPassword() != null) {
            String encryptedPassword = passwordEncoder.encode(userUpdateDTO.getPassword());
            user.setPassword(encryptedPassword);
        }

        if (userUpdateDTO.getPhone() != null) {
            user.setPhone(userUpdateDTO.getPhone());
        }

        if (userUpdateDTO.getRole() != null) {
            user.setRole(userUpdateDTO.getRole());
        }

        User updatedUser = userRepository.save(user);
        logger.info("Usuario {} actualizado", updatedUser.getId());

        return UserResponseDTO.from(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + id);
        }

        userRepository.deleteById(id);
        logger.info("Usuario {} eliminado", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ese email"));

        return UserResponseDTO.from(user);
    }
}
