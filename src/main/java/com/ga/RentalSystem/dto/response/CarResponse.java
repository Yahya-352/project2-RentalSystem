package com.ga.RentalSystem.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CarResponse(
        Long id,
        String make,
        String model,
        String category,
        String location,
        int year,
        String licensePlate,
        String transmission,
        String fuelType,
        int seats,
        BigDecimal pricePerDay,
        boolean available,
        String ownerUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}