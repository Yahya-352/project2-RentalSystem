package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.AgencyProfileRequest;
import com.ga.RentalSystem.dto.response.AgencyProfileResponse;
import com.ga.RentalSystem.service.AgencyProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles/agency")
@RequiredArgsConstructor
public class AgencyProfileController {

    private final AgencyProfileService agencyProfileService;

    @PostMapping
    public AgencyProfileResponse createProfile(
            @RequestBody AgencyProfileRequest request,
            Authentication authentication) {
        return agencyProfileService.createProfile(request, authentication);
    }

    @GetMapping("/me")
    public AgencyProfileResponse getMyProfile(Authentication authentication) {
        return agencyProfileService.getMyProfile(authentication);
    }

    @PutMapping("/me")
    public AgencyProfileResponse updateProfile(
            @RequestBody AgencyProfileRequest request,
            Authentication authentication) {
        return agencyProfileService.updateProfile(request, authentication);
    }
}