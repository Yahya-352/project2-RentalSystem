package com.ga.RentalSystem.dto.response;

import java.time.LocalDate;

public record BookingCalenderDatesResponse(LocalDate startDate , LocalDate endDate) {
}
