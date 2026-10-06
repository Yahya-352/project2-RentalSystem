package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.CarRequest;
import com.ga.RentalSystem.dto.response.CarResponse;
import com.ga.RentalSystem.dto.response.PageResponse;
import com.ga.RentalSystem.service.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;

    @PreAuthorize("hasRole('AGENCY')")
    @PostMapping("/create")
    public ResponseEntity<CarResponse> createCar(@RequestBody @Valid CarRequest carRequest ,
                                                 Authentication authentication){
        CarResponse response = carService.createCar(carRequest, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/")
    public PageResponse<CarResponse> getCars(@RequestParam(required = false) String location,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String make,
                                             @PageableDefault(size = 10) Pageable pageable) {
        return carService.getCars(location, category, make, pageable);
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
                                                 @RequestBody @Valid CarRequest carRequest,
                                                 Authentication authentication) {
        return carService.updateCar(id, carRequest, authentication);
    }

    @PreAuthorize("hasRole('AGENCY')")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    @DeleteMapping("/delete/{id}")
    public void deleteCar(@PathVariable Long id , Authentication authentication){
        carService.deleteCar(id , authentication);
    }

    @PreAuthorize("hasRole('AGENCY')")
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImage(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file,
                                            Authentication authentication) {
        carService.uploadImage(id, file, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
