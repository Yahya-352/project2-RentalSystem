package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.response.CityResponse;
import com.ga.RentalSystem.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
@Tag(name = "Cities", description = "Cities that cars can be located in, per country")
public class CityController {

    private final CityService cityService;

    @GetMapping
    @Operation(summary = "Get the cities of a country (public)")
    public List<CityResponse> getCities(@RequestParam Long countryId) {
        return cityService.getCities(countryId);
    }
}
