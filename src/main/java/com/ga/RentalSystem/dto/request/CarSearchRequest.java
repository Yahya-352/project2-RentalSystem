package com.ga.RentalSystem.dto.request;

import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CarSearchRequest(
        Long cityId,
        Long countryId,
        Long categoryId,
        Long makeId,
        @PositiveOrZero BigDecimal minPrice,
        @Positive BigDecimal maxPrice,
        TransmissionType transmission,
        FuelType fuelType,
        Integer minSeats
) {}
