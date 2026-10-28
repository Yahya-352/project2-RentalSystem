package com.ga.RentalSystem.specification;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.model.Car;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reusable filters for the car search. Each method returns one condition (one part of the SQL
 * {@code WHERE}); CarService combines the ones the user sent with {@code Specification.and(...)}.
 */
public class CarSpecification {

    /** Only static methods: never instantiated. */
    private CarSpecification() {

    }

    /** Cars that have not been soft-deleted. Always applied. */
    public static Specification<Car> notDeleted(){
        return ((root, query, cb) -> cb.isFalse(root.get("deleted")));
    }

    /** Cars located in the given city. */
    public static Specification<Car> hasCity(Long cityId){
        return ((root, query, cb)
                -> cb.equal(root.get("city").get("id") ,  cityId));
    }

    /** Cars located in any city of the given country (car → city → country). */
    public static Specification<Car> hasCountry(Long countryId){
        return (((root, query, cb) ->
                cb.equal(root.get("city").get("country").get("id") , countryId)));
    }

    /** Cars of the given category. */
    public static Specification<Car> hasCategory(Long categoryId){
        return ((root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId));
    }
    /** Cars of the given make. */
    public static Specification<Car> hasMake(Long makeId){
        return ((root, query, cb) ->
                cb.equal(root.get("make").get("id"), makeId));
    }
    /** Cars whose price per day is at most {@code maxPrice}. */
    public static Specification<Car> hasMaxPrice(BigDecimal maxPrice){
        return ((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPrice));
    }
    /** Cars whose price per day is at least {@code minPrice}. */
    public static Specification<Car> hasMinPrice(BigDecimal minPrice){
        return ((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("pricePerDay"), minPrice));
    }
    /** Cars with the given transmission. */
    public static Specification<Car> hasTransmissionType(TransmissionType transmissionType){
        return ((root, query, cb) ->
                cb.equal(root.get("transmission"), transmissionType));
    }
    /** Cars with the given fuel type. */
    public static Specification<Car> hasFuelType(FuelType fuelType){
        return ((root, query, cb) ->
                cb.equal(root.get("fuelType"), fuelType));
    }
    /** Cars with at least {@code minSeats} seats. */
    public static Specification<Car> minSeats(Integer minSeats){
        return ((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("seats"), minSeats));
    }
    /**
     * Cars that are free for the whole period: no APPROVED booking of the car overlaps it.
     * Same overlap rule as BookingService.isCarAvailable (a booking ending on {@code startDate}
     * does not clash). Becomes {@code NOT EXISTS (SELECT b.id FROM bookings b WHERE b.car_id = car.id
     * AND b.status = 'APPROVED' AND b.start_date < :endDate AND b.end_date > :startDate)}.
     *
     * @param startDate the requested pick-up date
     * @param endDate   the requested return date
     */
    public static Specification<Car> availableBetween(LocalDate startDate, LocalDate endDate){
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Booking> bookings = subquery.from(Booking.class);

            subquery.select(bookings.get("id"));

            subquery.where(
                    cb.equal(bookings.get("car") , root),
                    cb.equal(bookings.get("status") , BookingStatus.APPROVED),
                    cb.lessThan(bookings.get("startDate") ,endDate),
                    cb.greaterThan(bookings.get("endDate") ,startDate));

            return cb.not(cb.exists(subquery)); //return cars that do not exist in subquery
        };
    }

}
