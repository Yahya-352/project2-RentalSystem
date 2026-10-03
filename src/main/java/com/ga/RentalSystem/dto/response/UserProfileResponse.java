package com.ga.RentalSystem.dto.response;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String phoneNumber,
        String profilePictureUrl,
        String address,
        String licenseNumber,
        String licenseFileUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}