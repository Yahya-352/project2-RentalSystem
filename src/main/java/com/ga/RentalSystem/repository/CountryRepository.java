package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country , Long> {
}
