package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.AgencyProfileRequest;
import com.ga.RentalSystem.dto.response.AgencyProfileResponse;
import com.ga.RentalSystem.service.AgencyProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles/agency")
@RequiredArgsConstructor
public class AgencyProfileController {

    private final AgencyProfileService agencyProfileService;

    @PreAuthorize("hasRole('AGENCY')")
    @PostMapping
    public AgencyProfileResponse createProfile(
            @RequestBody AgencyProfileRequest request,
            Authentication authentication) {
        return agencyProfileService.createProfile(request, authentication);
    }

    @PreAuthorize("hasRole('AGENCY')")
    @GetMapping("/me")
    public AgencyProfileResponse getMyProfile(Authentication authentication) {
        return agencyProfileService.getMyProfile(authentication);
    }

    @PreAuthorize("hasRole('AGENCY')")
    @PutMapping("/me")
    public AgencyProfileResponse updateProfile(
            @RequestBody AgencyProfileRequest request,
            Authentication authentication) {
        return agencyProfileService.updateProfile(request, authentication);
    }
}