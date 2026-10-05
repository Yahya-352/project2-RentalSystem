package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.CarRequest;
import com.ga.RentalSystem.dto.response.CarResponse;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;

    @PreAuthorize("hasRole('AGENCY')")
    @PostMapping("/create")
    public ResponseEntity<CarResponse> createCar(@RequestBody CarRequest carRequest ,
                                                 Authentication authentication){
        return carService.createCar(carRequest , authentication);
    }

    @GetMapping("/")
    public List<CarResponse> getCars(){
        return carService.getCars();
    }

    @GetMapping("/{id}")
    public CarResponse getCarById(@PathVariable Long id) {
        return carService.getCarById(id);
    }

    @PreAuthorize("hasRole('AGENCY')")
    @GetMapping("/my-cars")
    public List<CarResponse> getMyCars(Authentication authentication) {
        return carService.getMyCars(authentication);
    }

    @PreAuthorize("hasRole('AGENCY')")
    @PutMapping("/update/{id}")
    public CarResponse updateCar(@PathVariable Long id,
                                                 @RequestBody CarRequest carRequest,
                                                 Authentication authentication) {
        return carService.updateCar(id, carRequest, authentication);
    }

    @PreAuthorize("hasRole('AGENCY')")
    @DeleteMapping("/delete/{id}")
    public void deleteCar(@PathVariable Long id , Authentication authentication){
        carService.deleteCar(id , authentication);
    }

}
