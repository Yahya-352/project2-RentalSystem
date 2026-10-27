package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> , JpaSpecificationExecutor<Car> {
    List<Car> findByOwnerId(Long OwnerId);
}
