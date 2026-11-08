package com.ga.user_service.dto.response;

import java.time.LocalDateTime;

public record AgencyProfileResponse(
        Long id,
        String businessName,
        String phoneNumber,
        String logoUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}