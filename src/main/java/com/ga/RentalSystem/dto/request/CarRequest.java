package com.ga.RentalSystem.dto.request;

import java.math.BigDecimal;

public record CarRequest(
        String make,
        String model,
        String category,
        String location,
        int year,
        String licensePlate,
        String transmission,
        String fuelType,
        int seats,
        BigDecimal pricePerDay
) {}