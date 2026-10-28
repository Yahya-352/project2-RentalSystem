package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking , Long> {
    List<Booking> findByRenterId(Long RenterId);

    List<Booking> findByCarId(Long carId);

    List<Booking> findByCarOwnerId(Long ownerId);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    List<Booking> findByCarIdAndStatusAndEndDateAfterOrderByStartDateAsc(Long carId,
                                                                    BookingStatus status,
                                                                    LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(Long id);

    @Modifying
    @Query("""
       update Booking b
       set b.status = :completed, b.updatedAt = CURRENT_TIMESTAMP
       where b.status = :approved and b.endDate < :today
       """)
    int completeFinishedBookings(BookingStatus approved, BookingStatus completed, LocalDate today);

}
