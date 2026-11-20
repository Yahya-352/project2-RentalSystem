package com.ga.payment_service.repository;

import com.ga.payment_service.enums.PaymentStatus;
import com.ga.payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Payment findBySessionId(String sessionId);

    Optional<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
}
