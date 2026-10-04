package com.ga.RentalSystem.dto.request;

public record AgencyProfileRequest(
        String businessName,
        String phoneNumber,
        String logoUrl
) {}