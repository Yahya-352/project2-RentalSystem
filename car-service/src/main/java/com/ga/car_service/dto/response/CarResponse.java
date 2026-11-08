package com.ga.car_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CarResponse(
        Long id,
        String make,
        String model,
        String category,
        String city,
        String country,
        int year,
        String licensePlate,
        String transmission,
        String fuelType,
        int seats,
        BigDecimal pricePerDay,
        boolean available,
        Long ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Long> imageIds
) {}