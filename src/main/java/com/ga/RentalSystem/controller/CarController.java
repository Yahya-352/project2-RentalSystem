package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.CarRequest;
import com.ga.RentalSystem.dto.response.CarResponse;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;

    @PostMapping("/create")
    public ResponseEntity<CarResponse> createCar(CarRequest carRequest ,
                                                 Authentication authentication){
        return carService.createCar(carRequest , authentication);
    }
    @GetMapping("/")
    public List<CarResponse> getCars(){
        return carService.getCars();
    }
    @GetMapping("/{id}")
    public ResponseEntity<CarResponse> getCarById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.getCarById(id));
    }

}
