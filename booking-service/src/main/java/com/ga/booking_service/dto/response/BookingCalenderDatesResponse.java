package com.ga.booking_service.dto.response;

import java.time.LocalDate;

public record BookingCalenderDatesResponse(LocalDate startDate , LocalDate endDate) {
}
