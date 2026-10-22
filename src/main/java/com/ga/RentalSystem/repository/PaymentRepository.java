package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Payment findBySessionId(String sessionId);
}
