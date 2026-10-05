package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car , Long> {
    List<Car> findByOwnerId(Long OwnerId);

    List<Car> findByDeletedFalse();

}
