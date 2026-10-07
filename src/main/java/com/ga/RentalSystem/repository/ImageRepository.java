package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByCarId(Long carId);
}