package com.ga.RentalSystem.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "makes")
public class Make {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , unique = true)
    private String name;

}
