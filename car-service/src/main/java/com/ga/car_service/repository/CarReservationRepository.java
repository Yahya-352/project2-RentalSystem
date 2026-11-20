package com.ga.car_service.repository;

import com.ga.car_service.model.CarReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarReservationRepository extends JpaRepository<CarReservation, Long> {
}
