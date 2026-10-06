package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.ChangePasswordRequest;
import com.ga.RentalSystem.dto.request.LoginRequest;
import com.ga.RentalSystem.dto.request.RegisterRequest;
import com.ga.RentalSystem.dto.request.ResetPasswordToken;
import com.ga.RentalSystem.dto.response.LoginResponse;
import com.ga.RentalSystem.dto.response.RegisterResponse;

import com.ga.RentalSystem.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerCustomer(@RequestBody @Valid RegisterRequest registerRequest){
        RegisterResponse response = userService.registerCustomer(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/agency")
    public ResponseEntity<RegisterResponse> registerAgency(@RequestBody @Valid RegisterRequest registerRequest){
        RegisterResponse response = userService.registerAgency(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest loginRequest){
        return userService.loginUser(loginRequest);
    }

    @GetMapping("/verify")
    public String verify(@RequestParam @NotBlank(message = "Token is required") String token){
        return userService.verify(token);
    }

    @PostMapping("/resend-verification")
    public String verifyReSend(@RequestParam @NotBlank(message = "Email is required")
                                                   @Email(message = "Email is not valid") String email){
        return userService.resendVerification(email);
    }

    @PostMapping("/forgot-password")
    public String passwordVerification(@RequestParam @NotBlank(message = "Email is required")
                                                           @Email(message = "Email is not valid") String email){
        return userService.passwordVerification(email);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody @Valid ResetPasswordToken resetPasswordToken){
        return userService.resetPassword(resetPasswordToken);
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestBody @Valid ChangePasswordRequest
                                                             changePasswordRequest){
        return userService.changePassword(changePasswordRequest);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    @PatchMapping("/{id}/deactivate")
    public void deactivateUser(@PathVariable Long id, Authentication authentication) {
        userService.deactivateUser(id, authentication);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT) //204 ..done no content to show
    @PatchMapping("/{id}/activate")
    public void activateUser(@PathVariable Long id, Authentication authentication) {
        userService.activateUser(id, authentication);
    }


}
