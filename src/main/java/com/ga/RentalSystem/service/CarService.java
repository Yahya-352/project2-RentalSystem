package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.CarRequest;
import com.ga.RentalSystem.dto.response.CarResponse;
import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.CarRepository;
import com.ga.RentalSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CarService {
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    public ResponseEntity<CarResponse> createCar(CarRequest carRequest ,
                                                   Authentication authentication){
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Car car = new Car();
        car.setMake(carRequest.make());
        car.setModel(carRequest.model());
        car.setCategory(carRequest.category());
        car.setLocation(carRequest.location());
        car.setYear(carRequest.year());
        car.setLicensePlate(carRequest.licensePlate());
        car.setSeats(carRequest.seats());
        car.setPricePerDay(carRequest.pricePerDay());
        car.setOwner(owner);
        car.setAvailable(true);

        try {
            car.setTransmission(TransmissionType.valueOf(carRequest.transmission().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid transmission type: " + carRequest.transmission());
        }

        try {
            car.setFuelType(FuelType.valueOf(carRequest.fuelType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid fuel type: " + carRequest.fuelType());
        }

        Car createdCar = carRepository.save(car);

        CarResponse carResponse = toCarResponse(createdCar);

        return ResponseEntity.status(HttpStatus.CREATED).body(carResponse);
    }

    public List<CarResponse> getCars(){
        List<Car> cars =  carRepository.findAll();
        return cars.stream().map(car ->toCarResponse(car)).toList();
    }

    public CarResponse getCarById(Long id){
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));
        return toCarResponse(car);
    }

    private CarResponse toCarResponse(Car car) {
        return new CarResponse(
                car.getId(),
                car.getMake(),
                car.getModel(),
                car.getCategory(),
                car.getLocation(),
                car.getYear(),
                car.getLicensePlate(),
                car.getTransmission().name(),
                car.getFuelType().name(),
                car.getSeats(),
                car.getPricePerDay(),
                car.isAvailable(),
                car.getOwner().getUserName()
        );
    }
}
