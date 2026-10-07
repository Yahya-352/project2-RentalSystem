package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.UserProfileRequest;
import com.ga.RentalSystem.dto.response.UserProfileResponse;
import com.ga.RentalSystem.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Customer Profiles", description = "Create, view and update a customer profile and upload a profile picture")
@RestController
@RequestMapping("/profiles/customer")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Create your customer profile (customer only)")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @RequestBody UserProfileRequest request,
            Authentication authentication) {
        UserProfileResponse response = userProfileService.createProfile(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "View your own profile (customer only)")
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/me")
    public UserProfileResponse getMyProfile(Authentication authentication){
        return userProfileService.getMyProfile(authentication);
    }

    @Operation(summary = "Update your own profile (customer only)")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/me")
    public UserProfileResponse updateProfile(
            @RequestBody UserProfileRequest request,
            Authentication authentication){
        return userProfileService.updateProfile(request, authentication);
    }



    @Operation(summary = "Upload your profile picture, JPG or PNG (customer only)")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping(value = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadProfilePicture(@RequestParam("file") MultipartFile file,
                                                     Authentication authentication) {
        userProfileService.uploadProfilePicture(file, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/picture")
    @Operation(summary = "Get the agency logo")
    public ResponseEntity<byte[]> getLogo(Authentication authentication) {
        byte[] logo = userProfileService.getUserProfilePicture(authentication);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(logo);
    }
}