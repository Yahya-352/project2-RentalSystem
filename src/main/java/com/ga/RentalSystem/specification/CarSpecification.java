package com.ga.RentalSystem.specification;

import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.model.City;
import com.ga.RentalSystem.model.Country;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

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

}
