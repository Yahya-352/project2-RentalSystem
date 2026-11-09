package com.ga.user_service.config;

import com.ga.user_service.enums.Role;
import com.ga.user_service.enums.UserStatus;
import com.ga.user_service.model.User;
import com.ga.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminMail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.seed.password}")
    private String seedPassword;

    @Override
    public void run(String... args) throws Exception {
        seedAdmin();
        seedUser("customer", "customer@rental.com", Role.CUSTOMER);
        seedUser("agency", "agency@rental.com", Role.AGENCY);

    }
    private void seedAdmin(){
        if(userRepository.findByEmail(adminMail).isPresent()){
            return;
        }
        User administrator = new User();
        administrator.setUserName("admin");
        administrator.setRoleEnum(Role.ADMIN);
        administrator.setEmail(adminMail);
        administrator.setVerified(true);
        administrator.setUserStatus(UserStatus.ACTIVE);
        administrator.setPassword(passwordEncoder.encode(adminPassword));
        userRepository.save(administrator);
        log.info("Admin Seeded");
    }

    private void seedUser(String userName, String email, Role role){
        if(userRepository.findByEmail(email).isPresent()){
            return;
        }
        User user = new User();
        user.setUserName(userName);
        user.setEmail(email);
        user.setRoleEnum(role);
        user.setVerified(true);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(seedPassword));
        userRepository.save(user);
        log.info(role + " Seeded");
    }

}
