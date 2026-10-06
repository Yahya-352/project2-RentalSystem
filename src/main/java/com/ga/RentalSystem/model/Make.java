package com.ga.RentalSystem.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "makes")
@Data
public class Make {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}