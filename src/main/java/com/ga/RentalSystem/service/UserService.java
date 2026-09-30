package com.ga.RentalSystem.service;


import com.ga.RentalSystem.dto.request.LoginRequest;
import com.ga.RentalSystem.dto.request.RegisterRequest;
import com.ga.RentalSystem.dto.response.LoginResponse;

import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.UserStatus;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.UserRepository;
import com.ga.RentalSystem.security.JWTUtils;
import com.ga.RentalSystem.security.MyUserDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserService(UserRepository userRepository,
                       EmailService emailService,
                       @Lazy PasswordEncoder passwordEncoder,
                       JWTUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
    }

    public User createUser(RegisterRequest request){
        User user = new User();
        user.setUserName(request.userName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoleEnum(Role.CUSTOMER);
        user.setUserStatus(UserStatus.UNVERIFIED);
        user.setVerified(false);

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);

        String link = "http://localhost:8080/users/verify?token=" + token;
        emailService.sendEmail(user.getEmail(), "Verify your account", "Click to verify: " + link);

        return userRepository.save(user);
    }

    public ResponseEntity<?> loginUser(LoginRequest loginRequest){
        try{
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email() , loginRequest.password()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            MyUserDetails myUserDetails = (MyUserDetails) authentication.getPrincipal();
            final String jwt = jwtUtils.generateJwtToken(myUserDetails);

            if (!myUserDetails.getUser().isVerified()) {
                return ResponseEntity.status(403)
                        .body("Please verify your email before logging in");
            }

            return ResponseEntity.ok(new LoginResponse(jwt ,
                    myUserDetails.getUsername(),
                    myUserDetails.getUser().getRoleEnum().name()));

        }catch (AuthenticationException authenticationException){
            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

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
