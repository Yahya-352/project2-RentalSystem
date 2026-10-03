package com.ga.RentalSystem.model;

import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, unique = true)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransmissionType transmission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelType fuelType;

    @Column(nullable = false)
    private int seats;

    @Column(nullable = false)
    private BigDecimal pricePerDay;

    @Column(nullable = false)
    private boolean available = true;
}