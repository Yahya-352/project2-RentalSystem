package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking , Long> {
    List<Booking> findByRenterId(Long RenterId);

    List<Booking> findByCarId(Long carId);

    List<Booking> findByCarOwnerId(Long ownerId);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

}
