package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.AgencyProfileRequest;
import com.ga.RentalSystem.dto.response.AgencyProfileResponse;
import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ForbiddenException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.AgencyProfile;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.AgencyProfileRepository;
import com.ga.RentalSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgencyProfileService {

    private final AgencyProfileRepository agencyProfileRepository;
    private final UserRepository userRepository;

    public AgencyProfileResponse createProfile(AgencyProfileRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        if (currentUser.getRoleEnum() != Role.AGENCY) {
            throw new ForbiddenException("Only agencies can create an agency profile");
        }

        if (agencyProfileRepository.existsByUserId(currentUser.getId())) {
            throw new BadRequestException("Profile already exists for this agency");
        }

        AgencyProfile profile = new AgencyProfile();
        profile.setUser(currentUser);
        profile.setBusinessName(request.businessName());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setLogoUrl(request.logoUrl());

        AgencyProfile saved = agencyProfileRepository.save(profile);
        return toResponse(saved);
    }

    public AgencyProfileResponse getMyProfile(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        AgencyProfile profile = agencyProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new InformationNotFoundException("Profile not found"));
        return toResponse(profile);
    }

    public AgencyProfileResponse updateProfile(AgencyProfileRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        AgencyProfile profile = agencyProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new InformationNotFoundException("Profile not found"));

        profile.setBusinessName(request.businessName());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setLogoUrl(request.logoUrl());

        AgencyProfile updated = agencyProfileRepository.save(profile);
        return toResponse(updated);
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));
    }

    private AgencyProfileResponse toResponse(AgencyProfile profile) {
        return new AgencyProfileResponse(
                profile.getId(),
                profile.getBusinessName(),
                profile.getPhoneNumber(),
                profile.getLogoUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}