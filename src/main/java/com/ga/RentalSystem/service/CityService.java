package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.response.CityResponse;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.City;
import com.ga.RentalSystem.repository.CityRepository;
import com.ga.RentalSystem.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only access to the cities cars can be located in. They are seeded from
 * {@code data/countries+cities.json} at startup (see DataSeeder).
 */
@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    /**
     * Returns the cities of one country, for the city dropdown shown after a country is picked.
     *
     * @param countryId the id of the country
     * @return the country's cities
     * @throws InformationNotFoundException if the country does not exist
     */
    public List<CityResponse> getCities(Long countryId) {
        if (!countryRepository.existsById(countryId)) {
            throw new InformationNotFoundException("Country not found with id: " + countryId);
        }

        List<City> cities = cityRepository.findByCountryId(countryId);

        return cities.stream()
                .map(city -> toResponse(city))
                .toList();
    }

    private CityResponse toResponse(City city) {
        return new CityResponse(city.getId(), city.getName());
    }
}
