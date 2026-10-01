package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.LoginRequest;
import com.ga.RentalSystem.dto.request.RegisterRequest;
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
    public User Register(@RequestBody RegisterRequest registerRequest){
        return userService.createUser(registerRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        return userService.loginUser(loginRequest);
    }

    @GetMapping("/verify")
    public String verify(@RequestParam String token){
        return userService.verify(token);
    }

    @GetMapping("/verify-resend")
    public String verifyReSend(@RequestBody String email){
        return userService.resendVerification(email);
    }

    @PostMapping("/forgot-password")
    public String passwordVerification(@RequestBody String email){
        return userService.passwordVerification(email);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token ,@RequestBody String password){
        return userService.resetPassword(password , token);
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
