package com.ga.booking_service.client;

import java.math.BigDecimal;

public record CarSummary(
        Long id,
        Long ownerId,
        BigDecimal pricePerDay,
        boolean available
) {
}
