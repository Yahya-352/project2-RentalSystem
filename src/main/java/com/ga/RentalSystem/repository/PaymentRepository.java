package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.enums.PaymentStatus;
import com.ga.RentalSystem.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findBySessionId(String sessionId);


    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
}
