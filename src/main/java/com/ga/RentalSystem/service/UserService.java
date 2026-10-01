package com.ga.RentalSystem.service;


import com.ga.RentalSystem.dto.request.LoginRequest;
import com.ga.RentalSystem.dto.request.RegisterRequest;
import com.ga.RentalSystem.dto.request.ResetPasswordToken;
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

import java.time.LocalDateTime;
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

    public String verify(String token){
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if(user.getVerificationTokenExpiryDate().isBefore(LocalDateTime.now())){
            return "Verification link expired";
        }
        user.setVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        return "Account verified!";
    }

    public User createUser(RegisterRequest request){
        User user = new User();
        user.setUserName(request.userName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoleEnum(Role.CUSTOMER);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setVerified(false);

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiryDate(LocalDateTime.now().plusHours(1));

        String link = "http://localhost:8080/users/verify?token=" + token;
        emailService.sendEmail(user.getEmail(), "Verify your account",
                "Click to verify: " + link);

        return userRepository.save(user);
    }

    public String resendVerification(String email){
        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new RuntimeException("No User Found with that email"));

        if(user.isVerified()){
            return "Account is Already Verified";
        }
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiryDate(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String link = "http://localhost:8080/users/verify?token=" + token;
        emailService.sendEmail(user.getEmail(), "Verify your account",
                "Click to verify: " + link);

        return "Email Sent Successfully";
    }

    public String passwordVerification(String email){
        //check if user exists
        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new RuntimeException("No User Found with that email"));

        //set token and its expiry date
        String passwordRecoveryToken = UUID.randomUUID().toString();
        LocalDateTime passwordRecoveryTokenExpiryDate = LocalDateTime.now().plusHours(1);

        //set values in DB
        user.setPasswordRecoveryToken(passwordRecoveryToken);
        user.setPasswordRecoveryTokenExpiryDate(passwordRecoveryTokenExpiryDate);
        userRepository.save(user);

        //send the email
        String link = "http://localhost:8080/users/reset-password?token=" + passwordRecoveryToken;
        emailService.sendEmail(user.getEmail(), "Recover your password",
                "Click to reset your password: " + link);

        return "Email Sent Successfully";
    }

    public String resetPassword(ResetPasswordToken resetPasswordToken){
        //check if token is real
        User user = userRepository.findByPasswordRecoveryToken(resetPasswordToken.token())
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        //check if user token is not expired and is verified
        if(user.getPasswordRecoveryTokenExpiryDate().isBefore(LocalDateTime.now())){
            return "Validation Token Expired";
        }
        if(!user.isVerified()){
            return "user has to be verified";
        }
        //set password and reset token values
        user.setPassword(passwordEncoder.encode(resetPasswordToken.password()));
        user.setPasswordRecoveryToken(null);
        user.setPasswordRecoveryTokenExpiryDate(null);
        userRepository.save(user);

        return "Password Updated";
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
