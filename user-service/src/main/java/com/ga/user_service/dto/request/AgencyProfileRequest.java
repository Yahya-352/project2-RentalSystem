package com.ga.user_service.dto.request;

public record AgencyProfileRequest(
        String businessName,
        String phoneNumber,
        String logoUrl
) {}