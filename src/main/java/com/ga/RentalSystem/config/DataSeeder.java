package com.ga.RentalSystem.config;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.TransmissionType;
import com.ga.RentalSystem.enums.UserStatus;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.model.Category;
import com.ga.RentalSystem.model.Make;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.BookingRepository;
import com.ga.RentalSystem.repository.CarRepository;
import com.ga.RentalSystem.repository.CategoryRepository;
import com.ga.RentalSystem.repository.MakeRepository;
import com.ga.RentalSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MakeRepository makeRepository;
    private final CategoryRepository categoryRepository;
    private final CarRepository carRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminMail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.seed.password}")
    private String seedPassword;

    @Override
    public void run(String... args) throws Exception {
        seedAdmin();
        seedCategories();
        seedMakes();
        seedUser("customer", "customer@rental.com", Role.CUSTOMER);
        seedUser("agency", "agency@rental.com", Role.AGENCY);
        seedCars();
        seedBookings();
    }
    private void seedAdmin(){
        if(userRepository.findByEmail(adminMail).isPresent()){
            return;
        }
        User administrator = new User();
        administrator.setUserName("admin");
        administrator.setRoleEnum(Role.ADMIN);
        administrator.setEmail(adminMail);
        administrator.setVerified(true);
        administrator.setUserStatus(UserStatus.ACTIVE);
        administrator.setPassword(passwordEncoder.encode(adminPassword));
        userRepository.save(administrator);
        log.info("Admin Seeded");
    }
    private void seedCategories(){
        if(categoryRepository.count() > 0){
            return;
        }
        String[] categories = {"Economy", "Sedan", "SUV", "Luxury", "Van", "Convertible"};
        for(String name : categories){
            Category category = new Category();
            category.setName(name);
            categoryRepository.save(category);
        }
        log.info("Categories Seeded");

    }

    private void seedMakes(){
        if(makeRepository.count() > 0){
            return;
        }
        String[] makes = {"Toyota", "Hyundai", "Kia", "Ford", "Nissan",
                "Honda", "Tesla", "BMW", "Mercedes-Benz"};
        for(String name : makes){
            Make make = new Make();
            make.setName(name);
            makeRepository.save(make);
        }
        log.info("Makes Seeded");
    }

    private void seedUser(String userName, String email, Role role){
        if(userRepository.findByEmail(email).isPresent()){
            return;
        }
        User user = new User();
        user.setUserName(userName);
        user.setEmail(email);
        user.setRoleEnum(role);
        user.setVerified(true);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(seedPassword));
        userRepository.save(user);
        log.info(role + " Seeded");
    }

    // license plates of the seeded cars, used to tell them apart from cars added through the app
    private static final List<String> SEED_PLATES = List.of(
            "SEED-001", "SEED-002", "SEED-003", "SEED-004", "SEED-005", "SEED-006");

    private void seedCars(){
        User agency = userRepository.findByEmail("agency@rental.com").orElse(null);
        if(agency == null){
            return;
        }

        Set<String> existingPlates = carRepository.findAll().stream()
                .map(Car::getLicensePlate)
                .collect(Collectors.toSet());

        //plate, make, model, category, location, year, transmission, fuel, seats, price per day (BHD)
        Object[][] cars = {
                {"SEED-001", "Toyota", "Corolla", "Economy", "Manama", 2023, TransmissionType.AUTOMATIC, FuelType.PETROL, 5, "12.000"},
                {"SEED-002", "Hyundai", "Tucson", "SUV", "Manama", 2024, TransmissionType.AUTOMATIC, FuelType.PETROL, 5, "18.500"},
                {"SEED-003", "Nissan", "Altima", "Sedan", "Muharraq", 2022, TransmissionType.AUTOMATIC, FuelType.PETROL, 5, "14.000"},
                {"SEED-004", "Tesla", "Model 3", "Luxury", "Riffa", 2024, TransmissionType.AUTOMATIC, FuelType.ELECTRIC, 5, "35.000"},
                {"SEED-005", "Kia", "Carnival", "Van", "Muharraq", 2023, TransmissionType.AUTOMATIC, FuelType.DIESEL, 8, "25.000"},
                {"SEED-006", "BMW", "Z4", "Convertible", "Manama", 2022, TransmissionType.MANUAL, FuelType.PETROL, 2, "45.000"}
        };

        int seeded = 0;
        for(Object[] row : cars){
            String plate = (String) row[0];
            if(existingPlates.contains(plate)){
                continue;
            }
            //looked up by name, because ids depend on the order rows were inserted
            Make make = findMake((String) row[1]);
            Category category = findCategory((String) row[3]);
            if(make == null || category == null){
                log.warn("Skipping seed car " + plate + ": make or category not found");
                continue;
            }

            Car car = new Car();
            car.setLicensePlate(plate);
            car.setMake(make);
            car.setModel((String) row[2]);
            car.setCategory(category);
            car.setLocation((String) row[4]);
            car.setYear((Integer) row[5]);
            car.setTransmission((TransmissionType) row[6]);
            car.setFuelType((FuelType) row[7]);
            car.setSeats((Integer) row[8]);
            car.setPricePerDay(new BigDecimal((String) row[9]));
            car.setOwner(agency);
            car.setAvailable(true);
            carRepository.save(car);
            seeded++;
        }
        if(seeded > 0){
            log.info(seeded + " Cars Seeded");
        }
    }

    private void seedBookings(){
        User customer = userRepository.findByEmail("customer@rental.com").orElse(null);
        if(customer == null){
            return;
        }

        List<Car> seedCars = carRepository.findAll().stream()
                .filter(car -> SEED_PLATES.contains(car.getLicensePlate()) && !car.isDeleted())
                .toList();

        //already seeded (or booked through the app) on an earlier run, so restarting never duplicates them
        boolean alreadyBooked = seedCars.stream()
                .anyMatch(car -> !bookingRepository.findByCarId(car.getId()).isEmpty());
        if(seedCars.isEmpty() || alreadyBooked){
            return;
        }

        //one sample booking of each status, with dates relative to today so they always make sense
        LocalDate today = LocalDate.now();
        Object[][] bookings = {
                //plate, start offset (days from today), length in days, status
                {"SEED-001", -20, 4, BookingStatus.COMPLETED},
                {"SEED-001", 5, 3, BookingStatus.APPROVED},
                {"SEED-002", 3, 4, BookingStatus.PENDING},
                {"SEED-003", 10, 2, BookingStatus.REJECTED},
                {"SEED-004", 2, 2, BookingStatus.CANCELLED},
                {"SEED-005", 14, 5, BookingStatus.PENDING}
        };

        int seeded = 0;
        for(Object[] row : bookings){
            Car car = seedCars.stream()
                    .filter(c -> c.getLicensePlate().equals(row[0]))
                    .findFirst().orElse(null);
            if(car == null){
                continue;
            }

            LocalDate start = today.plusDays((Integer) row[1]);
            LocalDate end = start.plusDays((Integer) row[2]);
            long days = ChronoUnit.DAYS.between(start, end);

            Booking booking = new Booking();
            booking.setRenter(customer);
            booking.setCar(car);
            booking.setStartDate(start);
            booking.setEndDate(end);
            booking.setTotalPrice(car.getPricePerDay().multiply(BigDecimal.valueOf(days)));
            booking.setStatus((BookingStatus) row[3]);
            bookingRepository.save(booking);
            seeded++;
        }
        if(seeded > 0){
            log.info(seeded + " Bookings Seeded");
        }
    }

    private Make findMake(String name){
        return makeRepository.findAll().stream()
                .filter(make -> make.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private Category findCategory(String name){
        return categoryRepository.findAll().stream()
                .filter(category -> category.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }
}
