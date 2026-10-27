package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> , JpaSpecificationExecutor<Car> {
    List<Car> findByOwnerId(Long OwnerId);

    Page<Car> findByDeletedFalse(Pageable pageable);

    Page<Car> findByDeletedFalseAndCityId(Long cityId, Pageable pageable);

    Page<Car> findByDeletedFalseAndCategoryNameIgnoreCase(String categoryName, Pageable pageable);

    Page<Car> findByDeletedFalseAndMakeNameIgnoreCase(String makeName, Pageable pageable);
}