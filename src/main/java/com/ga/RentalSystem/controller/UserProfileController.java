package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.UserProfileRequest;
import com.ga.RentalSystem.dto.response.UserProfileResponse;
import com.ga.RentalSystem.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles/customer")
@RequiredArgsConstructor

public class UserProfileController {

    private final UserProfileService userProfileService;


    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public UserProfileResponse createProfile(
            @RequestBody UserProfileRequest request,
            Authentication authentication) {
        return userProfileService.createProfile(request, authentication);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/me")
    public UserProfileResponse getMyProfile(Authentication authentication){
        return userProfileService.getMyProfile(authentication);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/me")
    public UserProfileResponse updateProfile(
            @RequestBody UserProfileRequest request,
            Authentication authentication){
        return userProfileService.updateProfile(request,authentication);
    }
}
