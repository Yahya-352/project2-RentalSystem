package com.ga.RentalSystem.service;


import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.UserStatus;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user){
        user.setRoleEnum(Role.CUSTOMER);
        user.setUserStatus(UserStatus.UNVERIFIED);
        return userRepository.save(user);
    }

    public User findUserByEmailAddress(String email){
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("no user found with email:" + email)
        );
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}
