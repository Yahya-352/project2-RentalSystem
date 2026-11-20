package com.ga.booking_service.specification;

import com.ga.booking_service.enums.BookingStatus;
import com.ga.booking_service.model.Booking;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Reusable filters for the admin booking list. Each method returns one condition (one part of
 * the SQL {@code WHERE}); BookingService combines the ones the admin sent with
 * {@code Specification.and(...)}.
 */
public class BookingSpecification {

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Booking> hasOwner(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<Booking> hasRenter(Long renterId) {
        return (root, query, cb) -> cb.equal(root.get("renterId"), renterId);
    }

    public static Specification<Booking> hasCar(Long carId) {
        return (root, query, cb) -> cb.equal(root.get("carId"), carId);
    }

    public static Specification<Booking> isPaid(boolean paid) {
        return (root, query, cb) -> cb.equal(root.get("paid"), paid);
    }

    /**
     * Bookings whose rental period overlaps the given range, using the same rule as the
     * availability check: a booking ending on {@code from} does not count.
     */
    public static Specification<Booking> overlaps(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.and(
                cb.lessThan(root.get("startDate"), to),
                cb.greaterThan(root.get("endDate"), from));
    }

    public static Specification<Booking> startsOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    public static Specification<Booking> endsOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), to);
    }
}
