package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.response.CityResponse;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.City;
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
