package com.ga.RentalSystem.config;

import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.UserStatus;
import com.ga.RentalSystem.model.Category;
import com.ga.RentalSystem.model.Make;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.CategoryRepository;
import com.ga.RentalSystem.repository.MakeRepository;
import com.ga.RentalSystem.repository.UserRepository;
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
    private final MakeRepository makeRepository;
    private final CategoryRepository categoryRepository;
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
        seedCategories();
        seedMakes();
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
    private void seedCategories(){
        if(categoryRepository.count() > 0){
            return;
        }
        String[] categories = {"Economy", "Sedan", "SUV", "Luxury", "Van", "Convertible"};
        for(String name : categories){
            Category category = new Category();
            category.setName(name);
            categoryRepository.save(category);
        }
        log.info("Categories Seeded");

    }

    private void seedMakes(){
        if(makeRepository.count() > 0){
            return;
        }
        String[] makes = {"Toyota", "Hyundai", "Kia", "Ford", "Nissan",
                "Honda", "Tesla", "BMW", "Mercedes-Benz"};
        for(String name : makes){
            Make make = new Make();
            make.setName(name);
            makeRepository.save(make);
        }
        log.info("Makes Seeded");
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
