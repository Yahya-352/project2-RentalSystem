package com.ga.payment_service.dto.response;

import com.ga.payment_service.enums.PaymentStatus;
import com.ga.payment_service.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSummaryResponse(
        Long id,
        Long bookingId,
        Long renterId,
        Long ownerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String make,
        String model,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentSummaryResponse from(Payment payment) {
        return new PaymentSummaryResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getRenterId(),
                payment.getOwnerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getMake(),
                payment.getModel(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
