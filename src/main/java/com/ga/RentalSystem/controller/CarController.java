package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.CarRequest;
import com.ga.RentalSystem.dto.response.CarResponse;
import com.ga.RentalSystem.dto.response.PageResponse;
import com.ga.RentalSystem.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Cars", description = "Create, browse, update and delete car listings")
@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;

    @Operation(summary = "Create a car listing (agency only)")

    @PreAuthorize("hasRole('AGENCY')")
    @PostMapping("/create")
    public ResponseEntity<CarResponse> createCar(@RequestBody @Valid CarRequest carRequest ,
                                                 Authentication authentication){
        CarResponse response = carService.createCar(carRequest, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List available cars (filter by location, category or make)")
    @GetMapping("/")
    public PageResponse<CarResponse> getCars(@RequestParam(required = false) String location,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String make,
                                             @PageableDefault(size = 10) Pageable pageable) {
        return carService.getCars(location, category, make, pageable);
    }

    @Operation(summary = "get a Car By its id")
    @GetMapping("/{id}")
    public CarResponse getCarById(@PathVariable Long id) {
        return carService.getCarById(id);
    }

    @Operation(summary = "View your own cars (owner(agency) of cars only)")
    @PreAuthorize("hasRole('AGENCY')")
    @GetMapping("/my-cars")
    public List<CarResponse> getMyCars(Authentication authentication) {
        return carService.getMyCars(authentication);
    }

    @Operation(summary = "update a car listing (Owner(Agency) of cars only)")
    @PreAuthorize("hasRole('AGENCY')")
    @PutMapping("/update/{id}")
    public CarResponse updateCar(@PathVariable Long id,
                                                 @RequestBody @Valid CarRequest carRequest,
                                                 Authentication authentication) {
        return carService.updateCar(id, carRequest, authentication);
    }

    @Operation(summary = "delete a car listing (Owner(agency) of cars only)")
    @PreAuthorize("hasRole('AGENCY')")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    @DeleteMapping("/delete/{id}")
    public void deleteCar(@PathVariable Long id , Authentication authentication){
        carService.deleteCar(id , authentication);
    }

    @Operation(summary = "Upload image of cars (agency only)")
    @PreAuthorize("hasRole('AGENCY')")
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImage(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file,
                                            Authentication authentication) {
        carService.uploadImage(id, file, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
