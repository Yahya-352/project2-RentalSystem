package com.ga.RentalSystem.dto.response;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.model.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long carId,
        String make,
        String model,
        Long renterId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalPrice,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean paid,
        boolean refunded
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
                booking.getCar().getId(),
                booking.getCar().getMake().getName(),
                booking.getCar().getModel(),
                booking.getRenter().getId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.isPaid(),
                booking.isRefunded());
    }
}