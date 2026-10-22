package com.barandgo.crm.identity.internal;

import com.barandgo.crm.identity.UserRequestDTO;
import com.barandgo.crm.identity.UserResponseDTO;
import com.barandgo.crm.identity.UserUpdateDTO;
import com.barandgo.crm.identity.User;
import com.barandgo.crm.identity.StaffTag;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ForbiddenException;
import com.barandgo.crm.shared.ResourceNotFoundException;
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserDirectory;
import com.barandgo.crm.identity.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService, UserDirectory {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    // Roles que un encargado puede crear, modificar o borrar
    private static final Set<UserRole> MANAGEABLE_BY_MANAGER = EnumSet.of(UserRole.STAFF, UserRole.CUSTOMER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDTO registerCustomer(UserRequestDTO userRequestDTO) {
        userRequestDTO.setRole(UserRole.CUSTOMER);
        userRequestDTO.setTags(null);
        return create(userRequestDTO);
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO, CurrentUser requester) {
        checkCanManage(requester, userRequestDTO.getRole());
        return create(userRequestDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return findUser(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        return UserResponseDTO.from(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserUpdateDTO userUpdateDTO, CurrentUser requester) {
        User user = findUser(id);

        // Hay que poder gestionar al usuario tal como está y, si cambia de rol, también el rol nuevo
        checkCanManage(requester, user.getRole());
        if (userUpdateDTO.getRole() != null) {
            checkCanManage(requester, userUpdateDTO.getRole());
        }

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

        if (userUpdateDTO.getTags() != null) {
            user.setTags(userUpdateDTO.getTags());
        }
        // Al pasar a cliente se pierden las etiquetas de puesto
        if (user.getRole() == UserRole.CUSTOMER) {
            user.setTags(null);
        }
        validateTags(user.getRole(), user.getTags());

        User updatedUser = userRepository.save(user);
        logger.info("Usuario {} actualizado", updatedUser.getId());

        return UserResponseDTO.from(updatedUser);
    }

    @Override
    public void deleteUser(Long id, CurrentUser requester) {
        User user = findUser(id);
        checkCanManage(requester, user.getRole());

        userRepository.delete(user);
        logger.info("Usuario {} eliminado", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ese email"));

        return UserResponseDTO.from(user);
    }

    private UserResponseDTO create(UserRequestDTO userRequestDTO) {
        if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }
        validateTags(userRequestDTO.getRole(), userRequestDTO.getTags());

        User user = new User();
        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        user.setPhone(userRequestDTO.getPhone());
        user.setRole(userRequestDTO.getRole());
        user.setTags(userRequestDTO.getTags());

        User savedUser = userRepository.save(user);
        logger.info("Usuario {} creado con rol {}", savedUser.getId(), savedUser.getRole());

        return UserResponseDTO.from(savedUser);
    }

    private static void checkCanManage(CurrentUser requester, UserRole targetRole) {
        if (requester.role() == UserRole.ADMIN) {
            return;
        }
        if (requester.role() == UserRole.MANAGER && MANAGEABLE_BY_MANAGER.contains(targetRole)) {
            return;
        }
        throw new ForbiddenException("No puedes gestionar usuarios con rol " + targetRole);
    }

    private static void validateTags(UserRole role, Set<StaffTag> tags) {
        if (role == UserRole.CUSTOMER && tags != null && !tags.isEmpty()) {
            throw new BadRequestException("Un cliente no puede tener etiquetas de puesto");
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }
}
