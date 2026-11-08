package com.ga.user_service.repository;

import com.ga.user_service.model.AgencyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgencyProfileRepository extends JpaRepository<AgencyProfile , Long> {
    Optional<AgencyProfile> findByUserId(Long id);
    boolean existsByUserId(Long userId);
}
