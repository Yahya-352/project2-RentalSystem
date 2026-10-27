package com.ga.RentalSystem.dto.request;

import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CarSearchRequest(
        Long cityId,
        Long countryId,
        Long categoryId,
        Long makeId,
        @PositiveOrZero BigDecimal minPrice,
        @Positive BigDecimal maxPrice,
        TransmissionType transmission,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,
        FuelType fuelType,
        Integer minSeats
) {}
