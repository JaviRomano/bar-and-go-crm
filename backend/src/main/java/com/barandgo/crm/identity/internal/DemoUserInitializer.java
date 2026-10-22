package com.barandgo.crm.identity.internal;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.barandgo.crm.identity.StaffTag;
import com.barandgo.crm.identity.User;
import com.barandgo.crm.identity.UserRole;

/**
 * Crea los usuarios de demostración si no hay ninguno. Solo en el perfil dev.
 * Las credenciales de demo son intencionadas y están documentadas en el README.
 */
@Component
@Profile("dev")
class DemoUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String managerPassword;
    private final String staffPassword;
    private final String customerPassword;

    DemoUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        @Value("${app.demo.admin-password}") String adminPassword,
                        @Value("${app.demo.manager-password}") String managerPassword,
                        @Value("${app.demo.staff-password}") String staffPassword,
                        @Value("${app.demo.customer-password}") String customerPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.managerPassword = managerPassword;
        this.staffPassword = staffPassword;
        this.customerPassword = customerPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Ya hay usuarios. Se omite la carga de demo.");
            return;
        }

        createUser("Admin Demo", "admin@bar.com", adminPassword, "+34912345678", UserRole.ADMIN, Set.of());
        createUser("Encargado Demo", "encargado@bar.com", managerPassword, "+34912345679", UserRole.MANAGER,
                Set.of(StaffTag.FLOOR));
        createUser("Personal Demo", "personal@bar.com", staffPassword, "+34912345680", UserRole.STAFF,
                Set.of(StaffTag.KITCHEN, StaffTag.BAR));
        createUser("Ellen Ripley", "ripleynostromo@example.com", customerPassword, "+34698765432", UserRole.CUSTOMER, Set.of());
        createUser("Lucas Trotacielos", "xwingluke@example.com", customerPassword, "+34611222333", UserRole.CUSTOMER, Set.of());
        createUser("Sarah Connor", "tepersigue@example.com", customerPassword, "+34655444555", UserRole.CUSTOMER, Set.of());

        logger.info("Usuarios de demo cargados: {}", userRepository.count());
    }

    private void createUser(String name, String email, String rawPassword, String phone, UserRole role,
                            Set<StaffTag> tags) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setRole(role);
        user.setTags(tags);
        userRepository.save(user);
    }
}
