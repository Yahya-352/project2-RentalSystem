package com.ga.RentalSystem.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CarRequest(
        @NotNull(message = "Make id is required")
        Long makeId,
        @NotBlank(message = "Model is required")
        String model,
        @NotNull(message = "Category id is required")
        Long categoryId,
        @NotBlank(message = "location is required")
        String location,
        @Min(value = 1980, message = "Year must be 1980 or later")
        @Max(value = 2100, message = "Year is not valid")
        int year,
        @NotBlank(message = "license Plate is required")
        String licensePlate,
        @NotBlank(message = "transmission is required")
        String transmission,
        @NotBlank(message = "fuelType is required")
        String fuelType,

        @Min(value = 1, message = "Seats must be at least 1")
        @Max(value = 20, message = "Seats must be at most 20")
        int seats,
        @NotNull(message = "pricePerDay is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal pricePerDay
) {}