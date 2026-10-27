package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.response.CountryResponse;
import com.ga.RentalSystem.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    public List<CountryResponse> getAllCountries() {
    }
}
