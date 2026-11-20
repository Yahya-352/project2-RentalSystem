package com.ga.car_service.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "car_reservations")
@Data
public class CarReservation {
    @Id
    private Long bookingId;

    @Column(nullable = false)
    private Long carId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;
}
