package com.ga.RentalSystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(unique = true , nullable = false)
    private String name;
    @Column(unique = true , nullable = false)
    private String iso2;
}
