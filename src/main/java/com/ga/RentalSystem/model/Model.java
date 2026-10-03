package com.ga.RentalSystem.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "models")
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "make_id", nullable = false)
    private Make make;

}
