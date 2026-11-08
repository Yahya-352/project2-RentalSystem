package com.ga.car_service.config;


import com.ga.car_service.model.*;
import com.ga.car_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MakeRepository makeRepository;
    private final CategoryRepository categoryRepository;
    private final CarRepository carRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final ImageRepository imageRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        seedCategories();
        seedMakes();
        seedCountries();
        seedCities();
//        seedCars();
        seedCarImages();
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



    private static final Path CAR_IMAGES = Paths.get("uploads/cars").toAbsolutePath().normalize();

    private static final List<String> SEED_PLATES = List.of(
            "SEED-001", "SEED-002", "SEED-003", "SEED-004", "SEED-005", "SEED-006");

//    private void seedCars(){
//        User agency = userRepository.findByEmail("agency@rental.com").orElse(null);
//        if(agency == null){
//            return;
//        }
//
//        Set<String> existingPlates = carRepository.findAll().stream()
//                .map(Car::getLicensePlate)
//                .collect(Collectors.toSet());
//
//        Object[][] cars = {
//                {"SEED-001", "Toyota", "Corolla", "Economy", "Manama", 2023, TransmissionType.AUTOMATIC, FuelType.PETROL, 5, "12.000"},
//                {"SEED-002", "Hyundai", "Tucson", "SUV", "Manama", 2024, TransmissionType.AUTOMATIC, FuelType.PETROL, 5, "18.500"},
//                {"SEED-003", "Nissan", "Altima", "Sedan", "Muharraq", 2022, TransmissionType.AUTOMATIC, FuelType.PETROL, 5, "14.000"},
//                {"SEED-004", "Tesla", "Model 3", "Luxury", "Riffa", 2024, TransmissionType.AUTOMATIC, FuelType.ELECTRIC, 5, "35.000"},
//                {"SEED-005", "Kia", "Carnival", "Van", "Muharraq", 2023, TransmissionType.AUTOMATIC, FuelType.DIESEL, 8, "25.000"},
//                {"SEED-006", "BMW", "Z4", "Convertible", "Manama", 2022, TransmissionType.MANUAL, FuelType.PETROL, 2, "45.000"}
//        };
//
//        int seeded = 0;
//        for(Object[] row : cars){
//            String plate = (String) row[0];
//            if(existingPlates.contains(plate)){
//                continue;
//            }
//            Make make = findMake((String) row[1]);
//            Category category = findCategory((String) row[3]);
//            City city = cityRepository.findByNameAndCountryIso2((String) row[4], "BH").orElse(null);
//            if(make == null || category == null || city == null){
//                log.warn("Skipping seed car " + plate + ": make, category or city not found");
//                continue;
//            }
//
//            Car car = new Car();
//            car.setLicensePlate(plate);
//            car.setMake(make);
//            car.setModel((String) row[2]);
//            car.setCategory(category);
//            car.setCity(city);
//            car.setYear((Integer) row[5]);
//            car.setTransmission((TransmissionType) row[6]);
//            car.setFuelType((FuelType) row[7]);
//            car.setSeats((Integer) row[8]);
//            car.setPricePerDay(new BigDecimal((String) row[9]));
//            car.setOwner(agency);
//            car.setAvailable(true);
//            carRepository.save(car);
//            seeded++;
//        }
//        if(seeded > 0){
//            log.info(seeded + " Cars Seeded");
//        }
//    }


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
