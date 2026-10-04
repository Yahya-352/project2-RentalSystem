package com.ga.RentalSystem.dto.response;

import com.ga.RentalSystem.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long carId,
        String carMake,
        String carModel,
        Long renterId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalPrice,
        BookingStatus status
) {}