package com.ga.RentalSystem.dto.request;

import java.time.LocalDate;

public record BookingRequest(
        Long carId,
        LocalDate startDate,
        LocalDate endDate
) {}