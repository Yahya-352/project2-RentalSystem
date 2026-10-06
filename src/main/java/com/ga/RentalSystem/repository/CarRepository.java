package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByOwnerId(Long OwnerId);

    Page<Car> findByDeletedFalse(Pageable pageable);
}