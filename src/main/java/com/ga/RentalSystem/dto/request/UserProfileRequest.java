package com.ga.RentalSystem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserProfileRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        String profilePictureUrl,
        String address,
        String licenseNumber,
        String licenseFileUrl
) {}