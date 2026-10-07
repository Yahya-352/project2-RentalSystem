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

/**
 * Business logic for car listings.
 * <p>
 * Agencies use this service to create, update, delete and upload images for their own cars.
 * Anyone can browse the cars that have not been deleted. Deleting a car is a soft delete:
 * the row stays in the database but is hidden from every query.
 */
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

    /**
     * Creates a new car listing owned by the logged-in user.
     * The car starts as available, and the action is written to the audit log.
     *
     * @param carRequest     the car details (make, category, model, location, year, plate,
     *                       transmission, fuel type, seats and price per day)
     * @param authentication the logged-in user, who becomes the owner of the car
     * @return the created car
     * @throws InformationNotFoundException if the user, make or category does not exist
     * @throws BadRequestException          if the transmission or fuel type is not a valid value
     */
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

    /**
     * Returns a page of cars that have not been deleted.
     * Only one filter is applied at a time, in this order: location, then category, then make.
     * If none is given, all non-deleted cars are returned.
     *
     * @param location the location to filter by (case-insensitive), or {@code null}
     * @param category the category name to filter by (case-insensitive), or {@code null}
     * @param make     the make name to filter by (case-insensitive), or {@code null}
     * @param pageable the page number, page size and sorting
     * @return a page of cars
     */
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

    /**
     * Returns one car by its id.
     *
     * @param id the id of the car
     * @return the car
     * @throws InformationNotFoundException if the car does not exist or has been deleted
     */
    public CarResponse getCarById(Long id){
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));
        if(car.isDeleted()){
            throw new InformationNotFoundException("Car Not Found");
        }
        return toCarResponse(car);
    }

    /**
     * Returns every car owned by the logged-in user, leaving out deleted cars.
     *
     * @param authentication the logged-in user
     * @return the cars owned by the user
     * @throws InformationNotFoundException if the user does not exist
     */
    public List<CarResponse> getMyCars(Authentication authentication){
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User Not Found"));
        List<Car> cars = carRepository.findByOwnerId(owner.getId());
        return cars.stream().filter(car -> !car.isDeleted()).map(car ->toCarResponse(car)).toList();
    }

    /**
     * Updates a car that belongs to the logged-in user, and writes the change to the audit log.
     *
     * @param id             the id of the car to update
     * @param carRequest     the new car details
     * @param authentication the logged-in user, who must be the owner
     * @return the updated car
     * @throws InformationNotFoundException if the user, car, make or category does not exist,
     *                                      or the car has been deleted
     * @throws ForbiddenException           if the car belongs to another user
     * @throws BadRequestException          if the transmission or fuel type is not a valid value
     */
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

    /**
     * Deletes a car that belongs to the logged-in user.
     * This is a soft delete: the car is marked as deleted and unavailable, and the row
     * stays in the database. The action is written to the audit log.
     *
     * @param id             the id of the car to delete
     * @param authentication the logged-in user, who must be the owner
     * @throws InformationNotFoundException if the user or car does not exist, or the car
     *                                      has already been deleted
     * @throws ForbiddenException           if the car belongs to another user
     */
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

    /**
     * Converts a {@link Car} entity into a {@link CarResponse}, so every method returns
     * the same shape.
     *
     * @param car the car entity
     * @return the response object
     */
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
                car.getUpdatedAt(),
                imageRepository.findByCarId(car.getId()).stream().map(image -> image.getId()).toList()
        );
    }

    /**
     * Uploads an image for a car that belongs to the logged-in user.
     * The file is validated and saved by {@link #saveFile(MultipartFile)}, and its stored
     * name is linked to the car in the images table.
     *
     * @param id             the id of the car
     * @param files           the images of cars (JPG or PNG)
     * @param authentication the logged-in user, who must be the owner
     * @throws InformationNotFoundException if the user or car does not exist, or the car
     *                                      has been deleted
     * @throws ForbiddenException           if the car belongs to another user
     * @throws BadRequestException          if the file is empty, is not a JPG or PNG, or
     *                                      cannot be stored
     */
    public void uploadImage(Long id, List<MultipartFile> files, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Car car = carRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("No files uploaded");
        }

        if (!car.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this car");
        }
        if (car.isDeleted()) {
            throw new InformationNotFoundException("Car not found");
        }
        for(MultipartFile file: files){
            String fileName = saveFile(file);

            Image image = new Image();
            image.setFileName(fileName);
            image.setCar(car);
            imageRepository.save(image);
        }
    }

    /** The folder where car images are stored. */
    private final Path rootLocation = Paths.get("uploads/cars").toAbsolutePath().normalize();

    /**
     * Validates an uploaded image and saves it to the car images folder.
     * The file type is checked from the first bytes of the file, not from its name or
     * content type. The file is stored under a random UUID name, and the final path is
     * checked to make sure it stays inside the upload folder.
     *
     * @param file the uploaded file
     * @return the random file name the image was stored under
     * @throws BadRequestException if the file is empty, is not a JPG or PNG, would be stored
     *                             outside the upload folder, or cannot be written
     */
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