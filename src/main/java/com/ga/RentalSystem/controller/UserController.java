package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.ChangePasswordRequest;
import com.ga.RentalSystem.dto.request.LoginRequest;
import com.ga.RentalSystem.dto.request.RegisterRequest;
import com.ga.RentalSystem.dto.request.ResetPasswordToken;
import com.ga.RentalSystem.model.User;

import com.ga.RentalSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> registerCustomer(@RequestBody RegisterRequest registerRequest){
        return userService.registerCustomer(registerRequest);
    }

    @PostMapping("/register/agency")
    public ResponseEntity<User> registerAgency(@RequestBody RegisterRequest registerRequest){
        return userService.registerAgency(registerRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        return userService.loginUser(loginRequest);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token){
        return userService.verify(token);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> verifyReSend(@RequestParam String email){
        return userService.resendVerification(email);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> passwordVerification(@RequestParam String email){
        return userService.passwordVerification(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordToken resetPasswordToken){
        return userService.resetPassword(resetPasswordToken);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest
                                                             changePasswordRequest){
        return userService.changePassword(changePasswordRequest);
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
