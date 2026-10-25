package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking , Long> {
    List<Booking> findByRenterId(Long RenterId);

    List<Booking> findByCarId(Long carId);

    List<Booking> findByCarOwnerId(Long ownerId);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(Long id);

}
