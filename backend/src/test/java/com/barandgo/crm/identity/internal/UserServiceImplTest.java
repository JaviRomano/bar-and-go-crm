package com.barandgo.crm.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.barandgo.crm.identity.UserRequestDTO;
import com.barandgo.crm.identity.UserResponseDTO;
import com.barandgo.crm.identity.UserUpdateDTO;
import com.barandgo.crm.identity.User;
import com.barandgo.crm.identity.StaffTag;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ForbiddenException;
import com.barandgo.crm.identity.CurrentUser;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final CurrentUser ADMIN = new CurrentUser(1L, "admin@bar.com", UserRole.ADMIN);
    private static final CurrentUser MANAGER = new CurrentUser(2L, "encargado@bar.com", UserRole.MANAGER);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerForcesCustomerRoleAndDropsTags() {
        mockSave();
        UserRequestDTO request = request(UserRole.ADMIN, Set.of(StaffTag.BAR));

        UserResponseDTO created = userService.registerCustomer(request);

        assertThat(created.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(created.getTags()).isEmpty();
    }

    @Test
    void managerCanCreateStaffWithTags() {
        mockSave();

        UserResponseDTO created = userService.createUser(request(UserRole.STAFF, Set.of(StaffTag.KITCHEN)), MANAGER);

        assertThat(created.getRole()).isEqualTo(UserRole.STAFF);
        assertThat(created.getTags()).containsExactly(StaffTag.KITCHEN);
    }

    @Test
    void managerCannotCreateManagerOrAdmin() {
        assertThatThrownBy(() -> userService.createUser(request(UserRole.MANAGER, null), MANAGER))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> userService.createUser(request(UserRole.ADMIN, null), MANAGER))
                .isInstanceOf(ForbiddenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void managerCannotPromoteStaffToManager() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(existing(10L, UserRole.STAFF)));
        UserUpdateDTO update = new UserUpdateDTO();
        update.setRole(UserRole.MANAGER);

        assertThatThrownBy(() -> userService.updateUser(10L, update, MANAGER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void managerCannotEditOrDeleteAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing(1L, UserRole.ADMIN)));
        UserUpdateDTO update = new UserUpdateDTO();
        update.setName("Cambio de nombre");

        assertThatThrownBy(() -> userService.updateUser(1L, update, MANAGER))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> userService.deleteUser(1L, MANAGER))
                .isInstanceOf(ForbiddenException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void adminCanPromoteToManager() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(existing(10L, UserRole.STAFF)));
        mockSave();
        UserUpdateDTO update = new UserUpdateDTO();
        update.setRole(UserRole.MANAGER);

        assertThat(userService.updateUser(10L, update, ADMIN).getRole()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void customerCannotHaveTags() {
        assertThatThrownBy(() -> userService.createUser(request(UserRole.CUSTOMER, Set.of(StaffTag.FLOOR)), ADMIN))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void demotingToCustomerClearsTags() {
        User staff = existing(10L, UserRole.STAFF);
        staff.setTags(Set.of(StaffTag.BAR));
        when(userRepository.findById(10L)).thenReturn(Optional.of(staff));
        mockSave();
        UserUpdateDTO update = new UserUpdateDTO();
        update.setRole(UserRole.CUSTOMER);

        assertThat(userService.updateUser(10L, update, ADMIN).getTags()).isEmpty();
    }

    private void mockSave() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static UserRequestDTO request(UserRole role, Set<StaffTag> tags) {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Nuevo");
        request.setEmail("nuevo@example.com");
        request.setPassword("secreta1");
        request.setPhone("+34600000000");
        request.setRole(role);
        request.setTags(tags);
        return request;
    }

    private static User existing(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName("Usuario " + id);
        user.setEmail("usuario" + id + "@example.com");
        user.setRole(role);
        return user;
    }
}
