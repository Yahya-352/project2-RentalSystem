package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.response.CountryResponse;
import com.ga.RentalSystem.model.Country;
import com.ga.RentalSystem.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only access to the countries cars can be located in. They are seeded from
 * {@code data/countries.json} at startup (see DataSeeder).
 */
@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    /**
     * Returns every country, for the country dropdowns.
     *
     * @return all countries
     */
    public List<CountryResponse> getAllCountries() {
        return countryRepository.findAll().stream()
                .map(country -> toResponse(country))
                .toList();
    }

    private CountryResponse toResponse(Country country) {
        return new CountryResponse(country.getId(), country.getName(), country.getIso2());
    }
}
