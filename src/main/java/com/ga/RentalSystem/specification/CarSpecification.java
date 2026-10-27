package com.ga.RentalSystem.specification;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.model.City;
import com.ga.RentalSystem.model.Country;
import com.stripe.model.tax.Registration;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.awt.print.Book;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CarSpecification {

    private CarSpecification() {

    }

    public static Specification<Car> notDeleted(){
        return ((root, query, cb) -> cb.isFalse(root.get("deleted")));
    }

    public static Specification<Car> hasCity(Long cityId){
        return ((root, query, cb)
                -> cb.equal(root.get("city").get("id") ,  cityId));
    }

    public static Specification<Car> hasCountry(Long countryId){
        return (((root, query, cb) ->
                cb.equal(root.get("city").get("country").get("id") , countryId)));
    }

    public static Specification<Car> hasCategory(Long categoryId){
        return ((root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId));
    }
    public static Specification<Car> hasMake(Long makeId){
        return ((root, query, cb) ->
                cb.equal(root.get("make").get("id"), makeId));
    }
    public static Specification<Car> hasMaxPrice(BigDecimal maxPrice){
        return ((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPrice));
    }
    public static Specification<Car> hasMinPrice(BigDecimal minPrice){
        return ((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("pricePerDay"), minPrice));
    }
    public static Specification<Car> hasTransmissionType(TransmissionType transmissionType){
        return ((root, query, cb) ->
                cb.equal(root.get("transmission"), transmissionType));
    }
    public static Specification<Car> hasFuelType(FuelType fuelType){
        return ((root, query, cb) ->
                cb.equal(root.get("fuelType"), fuelType));
    }
    public static Specification<Car> minSeats(Integer minSeats){
        return ((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("seats"), minSeats));
    }
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
