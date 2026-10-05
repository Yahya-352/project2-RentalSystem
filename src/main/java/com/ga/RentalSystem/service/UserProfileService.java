package com.ga.RentalSystem.service;


import com.ga.RentalSystem.dto.request.UserProfileRequest;
import com.ga.RentalSystem.dto.response.UserProfileResponse;
import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ForbiddenException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.model.UserProfile;
import com.ga.RentalSystem.repository.UserProfileRepository;
import com.ga.RentalSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserProfileResponse createProfile(UserProfileRequest request,
                                             Authentication authentication){
        User currentUser = getCurrentUser(authentication);

        if(currentUser.getRoleEnum() != Role.CUSTOMER){
            throw new ForbiddenException("Only customers can create a user profile");
        }
        if (userProfileRepository.existsByUserId(currentUser.getId())) {
            throw new BadRequestException("Profile already exists for this user");
        }

        UserProfile profile = new UserProfile();
        profile.setUser(currentUser);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setProfilePictureUrl(request.profilePictureUrl());
        profile.setAddress(request.address());
        profile.setLicenseNumber(request.licenseNumber());
        profile.setLicenseFileUrl(request.licenseFileUrl());

        UserProfile saved = userProfileRepository.save(profile);

        String message = "User " + currentUser.getId() + " created their profile";
        log.info(message);
        auditLogService.log(currentUser.getId(), "PROFILE_CREATED", "UserProfile", saved.getId(), message);

        return toResponse(saved);
    }

    public UserProfileResponse getMyProfile(Authentication authentication){
        User currentUser = getCurrentUser(authentication);

        UserProfile profile = userProfileRepository.findByUserId(currentUser.getId()).orElseThrow(
                ()->new InformationNotFoundException("Profile Not Found")
        );
        return toResponse(profile);
    }

    public UserProfileResponse updateProfile(UserProfileRequest request
            , Authentication authentication){
        User currentUser = getCurrentUser(authentication);

        UserProfile profile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new InformationNotFoundException("Profile not found"));

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setProfilePictureUrl(request.profilePictureUrl());
        profile.setAddress(request.address());
        profile.setLicenseNumber(request.licenseNumber());
        profile.setLicenseFileUrl(request.licenseFileUrl());

        UserProfile updated = userProfileRepository.save(profile);
        String message = "User " + currentUser.getId() + " updated their profile";
        log.info(message);
        auditLogService.log(currentUser.getId(), "PROFILE_UPDATED", "UserProfile", updated.getId(), message);

        return toResponse(updated);
    }

    private User getCurrentUser(Authentication authentication){
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhoneNumber(),
                profile.getProfilePictureUrl(),
                profile.getAddress(),
                profile.getLicenseNumber(),
                profile.getLicenseFileUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

}
