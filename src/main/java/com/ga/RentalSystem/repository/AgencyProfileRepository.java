package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.AgencyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Repository
public interface AgencyProfileRepository extends JpaRepository<AgencyProfile , Long> {
    Optional<AgencyProfile> findByUserId(Long id);
    boolean existsByUserId(Long userId);
}
