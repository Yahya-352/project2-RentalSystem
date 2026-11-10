package com.ga.booking_service.dto.response;

import com.ga.booking_service.enums.BookingStatus;
import com.ga.booking_service.model.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long carId,
        Long renterId,
        Long ownerId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalPrice,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Builds the response from a booking entity. It lives here, not in a service, so that both
     * BookingService and StripeService can use it without depending on each other.
     *
     * @param booking the booking entity
     * @return the response object
     */
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(),
                booking.getCarId(),
                booking.getRenterId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt());
    }
}