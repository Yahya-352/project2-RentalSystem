package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.CarRequest;
import com.ga.RentalSystem.dto.response.CarResponse;
import com.ga.RentalSystem.dto.response.PageResponse;
import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.TransmissionType;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ForbiddenException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.*;
import com.ga.RentalSystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j

@Service
@RequiredArgsConstructor
public class CarService {
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    private final AuditLogService auditLogService;
    private final CategoryRepository categoryRepository;
    private final MakeRepository makeRepository;

    private final ImageRepository imageRepository;
    //car creation method , user is required to submit car request dto and method
    // retrieves user email

    public CarResponse createCar(CarRequest carRequest ,
                                                   Authentication authentication){
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Car car = new Car();
        Make make = makeRepository.findById(carRequest.makeId())
                .orElseThrow(() -> new InformationNotFoundException("Make not found"));
        car.setMake(make);
        car.setModel(carRequest.model());
        Category category = categoryRepository.findById(carRequest.categoryId())
                .orElseThrow(() -> new InformationNotFoundException("Category not found"));
        car.setCategory(category);
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

        String message = "User " + owner.getId() + " created Car " + createdCar.getId();
        log.info(message);
        auditLogService.log(owner.getId(), "CAR_CREATED", "Car", createdCar.getId(), message);

        return toCarResponse(createdCar);
    }

    //get all cars method
    public PageResponse<CarResponse> getCars(String location, String category, String make, Pageable pageable) {
        Page<Car> carPage;

        if (location != null) {
            carPage = carRepository.findByDeletedFalseAndLocationIgnoreCase(location, pageable);
        } else if (category != null) {
            carPage = carRepository.findByDeletedFalseAndCategoryNameIgnoreCase(category, pageable);
        } else if (make != null) {
            carPage = carRepository.findByDeletedFalseAndMakeNameIgnoreCase(make, pageable);
        } else {
            carPage = carRepository.findByDeletedFalse(pageable);
        }

        Page<CarResponse> responsePage = carPage.map(car -> toCarResponse(car));
        return PageResponse.from(responsePage);
    }

    //get car by id method for 1 car retrieval
    public CarResponse getCarById(Long id){
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));
        if(car.isDeleted()){
            throw new InformationNotFoundException("Car Not Found");
        }
        return toCarResponse(car);
    }

    //this method makes owner based car retrieval
    public List<CarResponse> getMyCars(Authentication authentication){
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User Not Found"));
        List<Car> cars = carRepository.findByOwnerId(owner.getId());
        return cars.stream().filter(car -> !car.isDeleted()).map(car ->toCarResponse(car)).toList();
    }

    // update cars which are your own listing(not another person's)
    public CarResponse updateCar(Long id, CarRequest carRequest, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Car car = carRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));
        if(car.isDeleted()){
            throw new InformationNotFoundException("Car Not Found");
        }

        if (!car.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this car");
        }

        Make make = makeRepository.findById(carRequest.makeId())
                .orElseThrow(() -> new InformationNotFoundException("Make not found"));
        car.setMake(make);
        car.setModel(carRequest.model());
        Category category = categoryRepository.findById(carRequest.categoryId())
                .orElseThrow(() -> new InformationNotFoundException("Category not found"));
        car.setCategory(category);
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

        String message = "User " + currentUser.getId() + " updated Car " + car.getId();
        log.info(message);
        auditLogService.log(currentUser.getId(), "CAR_UPDATED", "Car", car.getId(), message);

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
        if(car.isDeleted()){
            throw new InformationNotFoundException("Car not found");
        }
        if(!car.getOwner().getId().equals(currentUser.getId())){
            throw new ForbiddenException("You Do Not Own This Car");
        }
        car.setDeleted(true);
        car.setAvailable(false);
        carRepository.save(car);
        String message = "User " + currentUser.getId() + " deleted Car " + car.getId();
        log.info(message);
        auditLogService.log(currentUser.getId(), "CAR_DELETED", "Car", car.getId(), message);
    }

    //template to reduce code as we will need to return car response on every method
    private CarResponse toCarResponse(Car car) {
        return new CarResponse(
                car.getId(),
                car.getMake().getName(),
                car.getModel(),
                car.getCategory().getName(),
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
    public void uploadImage(Long id, MultipartFile file, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Car car = carRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));

        if (!car.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this car");
        }
        if (car.isDeleted()) {
            throw new InformationNotFoundException("Car not found");
        }

        String fileName = saveFile(file);

        Image image = new Image();
        image.setFileName(fileName);
        image.setCar(car);
        imageRepository.save(image);
    }
    private final Path rootLocation = Paths.get("uploads/cars").toAbsolutePath().normalize();
    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file");
        }

        try {
            byte[] bytes = file.getBytes();

            boolean isJpg = bytes.length > 3
                    && bytes[0] == (byte) 0xFF
                    && bytes[1] == (byte) 0xD8
                    && bytes[2] == (byte) 0xFF;

            boolean isPng = bytes.length > 4
                    && bytes[0] == (byte) 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47;

            String extension;
            if (isJpg) {
                extension = ".jpg";
            } else if (isPng) {
                extension = ".png";
            } else {
                throw new BadRequestException("Only JPG and PNG images are allowed");
            }

            Files.createDirectories(rootLocation);

            String uniqueFileName = UUID.randomUUID() + extension;
            Path destinationFile = rootLocation.resolve(uniqueFileName).normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(rootLocation)) {
                throw new BadRequestException("Cannot store file outside current directory");
            }

            Files.write(destinationFile, bytes);
            return uniqueFileName;

        } catch (IOException e) {
            throw new BadRequestException("Failed to store file");
        }
    }
}
