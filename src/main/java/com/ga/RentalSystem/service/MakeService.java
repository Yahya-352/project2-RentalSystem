package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.MakeRequest;
import com.ga.RentalSystem.dto.response.MakeResponse;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.Make;
import com.ga.RentalSystem.repository.MakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MakeService {

    private final MakeRepository makeRepository;

    public List<MakeResponse> getAllMakes() {
        return makeRepository.findAll().stream()
                .map(make -> toResponse(make))
                .toList();
    }

    public MakeResponse getMakeById(Long id) {
        return toResponse(findMake(id));
    }

    public MakeResponse saveMake(MakeRequest request) {
        Make make = new Make();
        make.setName(request.name());
        return toResponse(makeRepository.save(make));
    }

    public MakeResponse updateMake(Long id, MakeRequest request) {
        Make make = findMake(id);
        make.setName(request.name());
        return toResponse(makeRepository.save(make));
    }

    public void deleteMake(Long id) {
        if (!makeRepository.existsById(id)) {
            throw new InformationNotFoundException("Make not found with id: " + id);
        }
        makeRepository.deleteById(id);
    }

    private Make findMake(Long id) {
        return makeRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Make not found with id: " + id));
    }

    private MakeResponse toResponse(Make make) {
        return new MakeResponse(make.getId(), make.getName());
    }
}