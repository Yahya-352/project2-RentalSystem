package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City,Long> {
    List<City> findByCountryId(Long id);
}
