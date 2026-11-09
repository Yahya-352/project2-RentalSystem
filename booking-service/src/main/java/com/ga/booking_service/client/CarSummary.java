package com.ga.booking_service.client;

import java.math.BigDecimal;

public record CarSummary(
        Long id,
        Long ownerId,
        BigDecimal dailyPrice,
        boolean available,
        boolean deleted
) {
}
