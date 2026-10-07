package com.ga.RentalSystem.service;


import com.ga.RentalSystem.dto.request.ChangePasswordRequest;
import com.ga.RentalSystem.dto.request.LoginRequest;
import com.ga.RentalSystem.dto.request.RegisterRequest;
import com.ga.RentalSystem.dto.request.ResetPasswordToken;
import com.ga.RentalSystem.dto.response.LoginResponse;

import com.ga.RentalSystem.dto.response.RegisterResponse;
import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.UserStatus;
import com.ga.RentalSystem.exceptions.*;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.UserRepository;
import com.ga.RentalSystem.security.JWTUtils;
import com.ga.RentalSystem.security.MyUserDetails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Business logic for user accounts: registration, email verification, login, password
 * recovery and password change, plus the admin actions to activate and deactivate users.
 * <p>
 * New users must verify their email address before they can log in. Agencies also need to
 * be approved by an admin, and until then their status is {@code UNAPPROVED_AGENCY}.
 * Important actions are written to the audit log.
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    private final AuditLogService auditLogService;   // new


    /**
     * Creates the service with the components it needs. The password encoder and the
     * authentication manager are injected lazily to avoid circular dependencies with the
     * security configuration.
     *
     * @param userRepository        access to the users table
     * @param emailService          sends verification and recovery emails
     * @param auditLogService       records important actions
     * @param passwordEncoder       hashes and checks passwords
     * @param jwtUtils              creates JWT tokens
     * @param authenticationManager checks the email and password at login
     */
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

    /**
     * Registers a new customer. See {@link #createUser(RegisterRequest, Role)}.
     *
     * @param request the registration details
     * @return the created user and a confirmation message
     * @throws ConflictException if the email is already registered
     */
    public RegisterResponse registerCustomer(RegisterRequest request) {
        return createUser(request, Role.CUSTOMER);
    }

    /**
     * Registers a new agency. The agency cannot log in until an admin approves it.
     * See {@link #createUser(RegisterRequest, Role)}.
     *
     * @param request the registration details
     * @return the created user and a confirmation message
     * @throws ConflictException if the email is already registered
     */
    public RegisterResponse registerAgency(RegisterRequest request) {
        return createUser(request, Role.AGENCY);
    }

    /**
     * Creates a new user with the given role. The password is stored hashed. Customers start
     * as {@code ACTIVE} and agencies as {@code UNAPPROVED_AGENCY}. Every user starts
     * unverified and is sent an email with a verification link that is valid for one hour.
     * The registration is written to the audit log.
     *
     * @param request the user name, email and password
     * @param role    the role of the new user
     * @return the created user and a confirmation message
     * @throws ConflictException if the email is already registered
     */
    public RegisterResponse createUser(RegisterRequest request , Role role){
        if(userRepository.findByEmail(request.email()).isPresent()){
            throw new ConflictException("Email Already Registered"); //409 - conflict
        }
        User user = new User();
        user.setUserName(request.userName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoleEnum(role);
        if (role == Role.AGENCY) {
            user.setUserStatus(UserStatus.UNAPPROVED_AGENCY);
        } else {
            user.setUserStatus(UserStatus.ACTIVE);
        }
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

        RegisterResponse response = new RegisterResponse(
                createdUser.getId(),
                createdUser.getUserName(),
                createdUser.getEmail(),
                createdUser.getUserStatus().name(),
                "Registration successful. Please check your email to verify your account."
        );
        return response;
    }

    /**
     * Verifies a user's email address using the token from the verification email.
     * The token is cleared after use, and the action is written to the audit log.
     *
     * @param token the verification token
     * @return a confirmation message
     * @throws InformationNotFoundException if no user has this token
     * @throws BadRequestException          if the token has expired
     */
    public String verify(String token){
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

        return "Account verified!";
    }

    /**
     * Sends a new verification email with a fresh token that is valid for one hour.
     *
     * @param email the email address of the user
     * @return a confirmation message
     * @throws InformationNotFoundException if no user has this email
     * @throws ConflictException            if the account is already verified
     */
    public String resendVerification(String email){
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

        return "Email Sent Successfully";
    }

    /**
     * Starts the password recovery flow. A recovery token that is valid for one hour is
     * saved for the user and sent to them by email.
     *
     * @param email the email address of the user
     * @return a confirmation message
     * @throws InformationNotFoundException if no user has this email
     */
    public String passwordVerification(String email){
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

        return "Email Sent Successfully";
    }

    /**
     * Sets a new password using the recovery token. The token is cleared after use, and the
     * action is written to the audit log.
     *
     * @param resetPasswordToken the recovery token and the new password
     * @return a confirmation message
     * @throws BadRequestException if the token is invalid or expired, or the user has not
     *                             verified their email
     */
    public String resetPassword(ResetPasswordToken resetPasswordToken){
        //check if token is real
        User user = userRepository.findByPasswordRecoveryToken(resetPasswordToken.token())
                .orElseThrow(() -> new BadRequestException("Invalid token"));

        //check if user token is not expired and is verified
        if(user.getPasswordRecoveryTokenExpiryDate().isBefore(LocalDateTime.now())){
            throw new BadRequestException("Validation Token Expired");
        }
        if(!user.isVerified()){
            throw new BadRequestException("Please Verify your email first");
        }
        //set password and reset token values
        user.setPassword(passwordEncoder.encode(resetPasswordToken.password()));
        user.setPasswordRecoveryToken(null);
        user.setPasswordRecoveryTokenExpiryDate(null);
        userRepository.save(user);

        String message = "User " + user.getId() + " reset their password";
        log.info(message);
        auditLogService.log(user.getId(), "PASSWORD_RESET", "User", user.getId(), message);

        return "Password Updated";
    }

    /**
     * Logs a user in and returns a JWT token. Spring Security checks the email and password
     * and also blocks accounts that are not allowed in: unapproved agencies are treated as
     * locked and deactivated users as disabled. Users who have not verified their email are
     * also refused.
     *
     * @param loginRequest the email and password
     * @return the JWT token, the user name and the role
     * @throws ForbiddenException     if the agency is waiting for approval, the account is
     *                                deactivated, or the email is not verified
     * @throws NotAuthorizedException if the email or password is wrong
     */
    public LoginResponse loginUser(LoginRequest loginRequest){
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
                throw new ForbiddenException("Please verify your email before logging in");
            }

            return new LoginResponse(jwt ,
                    myUserDetails.getUsername(),
                    myUserDetails.getUser().getRoleEnum().name());

        } catch (LockedException lockedException){
            log.warn("Login blocked for unapproved agency {}", loginRequest.email());
            throw new ForbiddenException("Your agency account is waiting for admin approval");
        }
        catch (DisabledException disabledException){
            log.warn("Login blocked for deactivated account {}", loginRequest.email());
            throw new ForbiddenException("Your account has been deactivated");
        }
        catch (AuthenticationException authenticationException){
            throw new NotAuthorizedException("Invalid email or password");
        }
    }

    /**
     * Changes the password of the logged-in user, after checking the current password.
     * The action is written to the audit log.
     *
     * @param changePasswordRequest the current password and the new password
     * @return a confirmation message
     * @throws BadRequestException if the current password is wrong
     */
    public String changePassword(ChangePasswordRequest changePasswordRequest){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUserDetails myUserDetails = (MyUserDetails) authentication.getPrincipal();
        User user = myUserDetails.getUser();

        if(!passwordEncoder.matches(changePasswordRequest.oldPassword() , user.getPassword())){
            throw new BadRequestException("Current Password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userRepository.save(user);

        String message = "User " + user.getId() + " changed their password";
        log.info(message);
        auditLogService.log(user.getId(), "PASSWORD_CHANGED", "User", user.getId(), message);

        return "Password changed successfully";
    }

    /**
     * Finds a user by email address.
     *
     * @param email the email address
     * @return the user
     * @throws UsernameNotFoundException if no user has this email (the exception type
     *                                   Spring Security expects when loading a user)
     */
    public User findUserByEmailAddress(String email){
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("no user found with email:" + email)
        );
    }

    /**
     * Deactivates a user account so that the user can no longer log in. Only admins can
     * call this, which is enforced in the controller. The action is written to the audit log.
     *
     * @param userId         the id of the user to deactivate
     * @param authentication the logged-in admin
     * @throws UsernameNotFoundException    if the admin cannot be found
     * @throws InformationNotFoundException if the user does not exist
     * @throws BadRequestException          if the admin tries to deactivate their own account
     * @throws ConflictException            if the user is already inactive
     */
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

    /**
     * Activates a user account. When the user is an agency waiting for approval, this
     * approves the agency and the audit log records it as {@code AGENCY_APPROVED}. Only
     * admins can call this, which is enforced in the controller.
     *
     * @param userId         the id of the user to activate
     * @param authentication the logged-in admin
     * @throws UsernameNotFoundException    if the admin cannot be found
     * @throws InformationNotFoundException if the user does not exist
     * @throws ConflictException            if the user is already active
     */
    public void activateUser(Long userId , Authentication authentication){

        User admin = findUserByEmailAddress(authentication.getName());

        User user = userRepository.findById(userId).orElseThrow(
                () -> new InformationNotFoundException("User not found")
        );
        if(user.getUserStatus() == UserStatus.ACTIVE){
            throw new ConflictException("User is already active");
        }

        String action = "USER_ACTIVATED";
        String message = "Admin " + admin.getId() + " activated User " + user.getId();

        if(user.getUserStatus() == UserStatus.UNAPPROVED_AGENCY){
            action = "AGENCY_APPROVED";
            message = "Admin " + admin.getId() + " approved Agency " + user.getId();
        }

        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info(message);
        auditLogService.log(admin.getId(), action, "User", user.getId(), message);
    }

}