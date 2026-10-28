package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.enums.PaymentStatus;
import com.ga.RentalSystem.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findBySessionId(String sessionId);

    Optional<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
}
