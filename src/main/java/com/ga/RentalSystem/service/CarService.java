package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.CarRequest;
import com.ga.RentalSystem.dto.response.CarResponse;
import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ForbiddenException;
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

    //car creation method , user is required to submit car request dto and method
    // retrieves user email
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

    //get all cars method
    public List<CarResponse> getCars(){
        List<Car> cars =  carRepository.findAll();
        return cars.stream().map(car ->toCarResponse(car)).toList();
    }

    //get car by id method for 1 car retrieval
    public CarResponse getCarById(Long id){
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));
        return toCarResponse(car);
    }

    //this method makes owner based car retrieval
    public List<CarResponse> getMyCars(Authentication authentication){
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User Not Found"));
        List<Car> cars = carRepository.findByOwnerId(owner.getId());
        return cars.stream().map(car ->toCarResponse(car)).toList();
    }

    // update cars which are your own listing(not another person's)
    public CarResponse updateCar(Long id, CarRequest carRequest, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Car car = carRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));

        if (!car.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this car");
        }

        car.setMake(carRequest.make());
        car.setModel(carRequest.model());
        car.setCategory(carRequest.category());
        car.setLocation(carRequest.location());
        car.setYear(carRequest.year());
        car.setLicensePlate(carRequest.licensePlate());
        car.setSeats(carRequest.seats());
        car.setPricePerDay(carRequest.pricePerDay());

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

        Car updatedCar = carRepository.save(car);
        return toCarResponse(updatedCar);
    }

    // this method deletes a car that belongs to the current logged owner
    public void deleteCar(Long id , Authentication authentication){
        User currentUser = userRepository.findByEmail(authentication.getName()).orElseThrow(
                () -> new InformationNotFoundException("User not Found")
        );
        Car car = carRepository.findById(id).orElseThrow(
                () -> new InformationNotFoundException("Car Not Found")
        );
        if(!car.getOwner().getId().equals(currentUser.getId())){
            throw new ForbiddenException("You Do Not Own This Car");
        }
        carRepository.delete(car);
    }



    //template to reduce code as we will need to return car response on every method
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
                car.getOwner().getUserName(),
                car.getCreatedAt(),
                car.getUpdatedAt()
        );
    }
}
