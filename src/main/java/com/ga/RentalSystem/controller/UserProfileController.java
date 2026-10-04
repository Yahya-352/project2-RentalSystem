package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.UserProfileRequest;
import com.ga.RentalSystem.dto.response.UserProfileResponse;
import com.ga.RentalSystem.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles/customer")
@RequiredArgsConstructor

public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping
    public UserProfileResponse createProfile(
            @RequestBody UserProfileRequest request,
            Authentication authentication) {
        return userProfileService.createProfile(request, authentication);
    }
}
