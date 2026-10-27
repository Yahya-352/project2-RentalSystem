package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.response.CountryResponse;
import com.ga.RentalSystem.model.Country;
import com.ga.RentalSystem.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    public List<CountryResponse> getAllCountries() {
        return countryRepository.findAll().stream()
                .map(country -> toResponse(country))
                .toList();
    }

    private CountryResponse toResponse(Country country) {
        return new CountryResponse(country.getId(), country.getName(), country.getIso2());
    }
}
