package com.ga.RentalSystem.config;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.enums.FuelType;
import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.TransmissionType;
import com.ga.RentalSystem.enums.UserStatus;
import com.ga.RentalSystem.model.*;
import com.ga.RentalSystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final ImageRepository imageRepository;
    private final ObjectMapper objectMapper;
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
        seedCountries();
        seedCities();
        seedCars();
        seedCarImages();
        seedBookings();
    }
    private List<Country> listCountries(){
        ClassPathResource resource = new ClassPathResource("data/countries.json");
        try(InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<Country>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private record CountriesCitiesJson(String name , List<String> cities){};
    private List<CountriesCitiesJson> listCities(){
        ClassPathResource resource = new ClassPathResource("data/countries+cities.json");
        try(InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<CountriesCitiesJson>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void seedCountries(){
        if (countryRepository.count() > 0){return;};
        countryRepository.saveAll(listCountries());
        log.info("Countries saved successfully");
    }
    private void seedCities(){
        if (cityRepository.count() > 0){
            return;
        }
        Map<String , Country> countriesByName = countryRepository.findAll().stream()
                .collect(Collectors.toMap(Country::getName , c -> c));
        List<City> cities = new ArrayList<>();
        for (CountriesCitiesJson entry : listCities()){
            Country country = countriesByName.get(entry.name());
            if (country == null){
                log.warn("Country not found: " + entry.name());
                continue;
            }
            for (String cityName : entry.cities()){
                City city = new City();
                city.setName(cityName);
                city.setCountry(country);
                cities.add(city);
            }
        }
        cityRepository.saveAll(cities);
        log.info(cities.size() + " Cities saved successfully");
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

    /** Same folder CarService saves uploaded car images to. */
    private static final Path CAR_IMAGES = Paths.get("uploads/cars").toAbsolutePath().normalize();

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
            Make make = findMake((String) row[1]);
            Category category = findCategory((String) row[3]);
            City city = cityRepository.findByNameAndCountryIso2((String) row[4], "BH").orElse(null);
            if(make == null || category == null || city == null){
                log.warn("Skipping seed car " + plate + ": make, category or city not found");
                continue;
            }

            Car car = new Car();
            car.setLicensePlate(plate);
            car.setMake(make);
            car.setModel((String) row[2]);
            car.setCategory(category);
            car.setCity(city);
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

        boolean alreadyBooked = seedCars.stream()
                .anyMatch(car -> !bookingRepository.findByCarId(car.getId()).isEmpty());
        if(seedCars.isEmpty() || alreadyBooked){
            return;
        }

        LocalDate today = LocalDate.now();
        Object[][] bookings = {
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

    /**
     * Gives every seeded car that has no photos yet its real photos from
     * src/main/resources/seed-images (SEED-001-1.jpg, SEED-001-2.jpg, ...). The files are
     * copied into uploads/cars under a random name, like an uploaded image. Credits for the
     * photos are in the README.
     */
    private void seedCarImages(){
        List<Car> seedCars = carRepository.findAll().stream()
                .filter(car -> SEED_PLATES.contains(car.getLicensePlate()))
                .toList();

        int seeded = 0;
        for(Car car : seedCars){
            if(!imageRepository.findByCarId(car.getId()).isEmpty()){
                continue;
            }
            for(int i = 1; i <= 2; i++){
                ClassPathResource resource = new ClassPathResource(
                        "seed-images/" + car.getLicensePlate() + "-" + i + ".jpg");
                if(!resource.exists()){
                    continue;
                }
                String fileName = UUID.randomUUID() + ".jpg";
                try(InputStream is = resource.getInputStream()){
                    Files.createDirectories(CAR_IMAGES);
                    Files.copy(is, CAR_IMAGES.resolve(fileName));
                } catch (IOException e){
                    log.warn("Could not copy seed image for " + car.getLicensePlate(), e);
                    continue;
                }
                Image image = new Image();
                image.setFileName(fileName);
                image.setCar(car);
                imageRepository.save(image);
                seeded++;
            }
        }
        if(seeded > 0){
            log.info(seeded + " Car images Seeded");
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
