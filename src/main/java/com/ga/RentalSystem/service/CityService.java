package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.response.CityResponse;
import com.ga.RentalSystem.repository.CityRepository;
import com.ga.RentalSystem.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    public List<CityResponse> getCities(Long countryId, String name) {

    }
}
