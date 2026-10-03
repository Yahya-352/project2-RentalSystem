package com.ga.RentalSystem.model;

import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role roleEnum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus;

    @Column
    private String verificationToken;

    @Column
    private LocalDateTime verificationTokenExpiryDate;

    @Column
    private boolean verified = false;

    @Column
    private String passwordRecoveryToken;

    @Column
    private LocalDateTime passwordRecoveryTokenExpiryDate;

    @OneToMany(mappedBy = "renter", cascade = CascadeType.ALL)
    private List<Booking> rentals = new ArrayList<>();

}
