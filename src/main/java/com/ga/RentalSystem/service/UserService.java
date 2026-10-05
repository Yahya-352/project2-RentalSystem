package com.ga.RentalSystem.service;


import com.ga.RentalSystem.dto.request.ChangePasswordRequest;
import com.ga.RentalSystem.dto.request.LoginRequest;
import com.ga.RentalSystem.dto.request.RegisterRequest;
import com.ga.RentalSystem.dto.request.ResetPasswordToken;
import com.ga.RentalSystem.dto.response.LoginResponse;

import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.UserStatus;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ConflictException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.UserRepository;
import com.ga.RentalSystem.security.JWTUtils;
import com.ga.RentalSystem.security.MyUserDetails;

import com.ga.RentalSystem.security.SecurityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;


@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    private final AuditLogService auditLogService;   // new


    @Autowired
    public UserService(UserRepository userRepository,
                       EmailService emailService,
                       AuditLogService auditLogService,
                       @Lazy PasswordEncoder passwordEncoder,
                       JWTUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
    }

    public ResponseEntity<User> registerCustomer(RegisterRequest request) {
        return createUser(request, Role.CUSTOMER);
    }

    public ResponseEntity<User> registerAgency(RegisterRequest request) {
        return createUser(request, Role.AGENCY);
    }

    public ResponseEntity<User> createUser(RegisterRequest request , Role role){
        if(userRepository.findByEmail(request.email()).isPresent()){
            throw new ConflictException("Email Already Registered");
        }
        User user = new User();
        user.setUserName(request.userName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoleEnum(role);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setVerified(false);

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiryDate(LocalDateTime.now().plusHours(1));

        String link = "http://localhost:8080/users/verify?token=" + token;

        emailService.sendEmail(user.getEmail(), "Verify your account",
                "Click to verify: " + link);
        User createdUser = userRepository.save(user);

        String message = "User " + createdUser.getId() + " registered as " + role;
        log.info(message);
        auditLogService.log(createdUser.getId(), "USER_REGISTERED", "User", createdUser.getId(), message);


        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    public ResponseEntity<String> verify(String token){
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new InformationNotFoundException("Invalid token"));

        if(user.getVerificationTokenExpiryDate().isBefore(LocalDateTime.now())){
            throw new BadRequestException("Verification Link Expired");
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        String message = "User " + user.getId() + " verified their email";
        log.info(message);
        auditLogService.log(user.getId(), "EMAIL_VERIFIED", "User", user.getId(), message);

        return ResponseEntity.ok().body("Account verified!");
    }

    public ResponseEntity<String> resendVerification(String email){
        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new InformationNotFoundException("No User Found with that email"));

        if(user.isVerified()){
            throw new ConflictException("Account Is Already Verified");
        }
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiryDate(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String link = "http://localhost:8080/users/verify?token=" + token;
        emailService.sendEmail(user.getEmail(), "Verify your account",
                "Click to verify: " + link);

        return ResponseEntity.ok().body("Email Sent Successfully");
    }

    public ResponseEntity<String> passwordVerification(String email){
        //check if user exists
        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new InformationNotFoundException
                        ("No User Found with that email"));

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

        return ResponseEntity.ok().body("Email Sent Successfully");
    }

    public ResponseEntity<String> resetPassword(ResetPasswordToken resetPasswordToken){
        //check if token is real
        User user = userRepository.findByPasswordRecoveryToken(resetPasswordToken.token())
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        //check if user token is not expired and is verified
        if(user.getPasswordRecoveryTokenExpiryDate().isBefore(LocalDateTime.now())){
            return ResponseEntity.ok().body("Validation Token Expired");
        }
        if(!user.isVerified()){
            return ResponseEntity.ok().body("Validation Token Expired");
        }
        //set password and reset token values
        user.setPassword(passwordEncoder.encode(resetPasswordToken.password()));
        user.setPasswordRecoveryToken(null);
        user.setPasswordRecoveryTokenExpiryDate(null);
        userRepository.save(user);

        String message = "User " + user.getId() + " reset their password";
        log.info(message);
        auditLogService.log(user.getId(), "PASSWORD_RESET", "User", user.getId(), message);


        return ResponseEntity.ok().body("Password Updated");
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

        } catch (DisabledException disabledException){
        log.warn("Login blocked for deactivated account {}", loginRequest.email());
        return ResponseEntity.status(403)
                .body("Your account has been deactivated");
        }

        catch (AuthenticationException authenticationException){
            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

    }

    public ResponseEntity<String> changePassword(ChangePasswordRequest changePasswordRequest){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUserDetails myUserDetails = (MyUserDetails) authentication.getPrincipal();
        User user = myUserDetails.getUser();

        if(!passwordEncoder.matches(changePasswordRequest.oldPassword() , user.getPassword())){
            return ResponseEntity.status(400).body("Current Password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userRepository.save(user);

        String message = "User " + user.getId() + " changed their password";
        log.info(message);
        auditLogService.log(user.getId(), "PASSWORD_CHANGED", "User", user.getId(), message);

        return ResponseEntity.ok("Password changed succesfully");
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

    public void deactivateUser(Long userId , Authentication authentication){

        User admin = findUserByEmailAddress(authentication.getName());

        User user = userRepository.findById(userId).orElseThrow(
                () -> new InformationNotFoundException("User not found")
        );

        if(admin.getId().equals(user.getId())){
            throw new BadRequestException("You cannot deactivate your own account");
        }
        if(user.getUserStatus() == UserStatus.INACTIVE){
            throw new ConflictException("User is already inactive");
        }

        user.setUserStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        String message = "Admin " + admin.getId() + " deactivated User " + user.getId();
        log.info(message);
        auditLogService.log(admin.getId(), "USER_DEACTIVATED", "User", user.getId(), message);
    }

    public void activateUser(Long userId , Authentication authentication){

        User admin = findUserByEmailAddress(authentication.getName());

        User user = userRepository.findById(userId).orElseThrow(
                () -> new InformationNotFoundException("User not found")
        );
        if(user.getUserStatus() == UserStatus.ACTIVE){
            throw new ConflictException("User is already active");
        }

        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String message = "Admin " + admin.getId() + " activated User " + user.getId();
        log.info(message);
        auditLogService.log(admin.getId(), "USER_ACTIVATED", "User", user.getId(), message);
    }

}
