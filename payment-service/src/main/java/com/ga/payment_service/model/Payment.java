package com.ga.payment_service.model;

import com.ga.payment_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long renterId;

    @Column
    private Long ownerId;

    @Column
    private Long bookingId;

    @Column(precision = 10, scale = 3)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = false , unique = true)
    private String sessionId;

    @Column
    private String paymentIntent;
}
