package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.AgencyProfileRequest;
import com.ga.RentalSystem.dto.response.AgencyProfileResponse;
import com.ga.RentalSystem.service.AgencyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Agency Profiles", description = "Create, view and update an agency profile")
@RestController
@RequestMapping("/profiles/agency")
@RequiredArgsConstructor
public class AgencyProfileController {

    private final AgencyProfileService agencyProfileService;

    @Operation(summary = "Create your agency profile (agency only)")
    @PreAuthorize("hasRole('AGENCY')")
    @PostMapping
    public ResponseEntity<AgencyProfileResponse> createProfile(
            @RequestBody AgencyProfileRequest request,
            Authentication authentication) {
        AgencyProfileResponse response = agencyProfileService.createProfile(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "View your own agency profile (agency only)")
    @PreAuthorize("hasRole('AGENCY')")
    @GetMapping("/me")
    public AgencyProfileResponse getMyProfile(Authentication authentication) {
        return agencyProfileService.getMyProfile(authentication);
    }

    @Operation(summary = "Update your own agency profile (agency only)")
    @PreAuthorize("hasRole('AGENCY')")
    @PutMapping("/me")
    public AgencyProfileResponse updateProfile(
            @RequestBody AgencyProfileRequest request,
            Authentication authentication) {
        return agencyProfileService.updateProfile(request, authentication);
    }
}