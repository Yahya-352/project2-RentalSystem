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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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
//        profile.setProfilePictureUrl(request.profilePictureUrl());
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
//        profile.setProfilePictureUrl(request.profilePictureUrl());
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
    public void uploadProfilePicture(MultipartFile file, Authentication authentication) {
        User user = getCurrentUser(authentication);

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new InformationNotFoundException("Create your profile first"));

        profile.setProfilePictureUrl(saveFile(file));
        userProfileRepository.save(profile);
    }


    private final Path rootLocation = Paths.get("uploads/userprofile").toAbsolutePath().normalize();
    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file");
        }

        try {
            byte[] bytes = file.getBytes();

            boolean isJpg = bytes.length > 3
                    && bytes[0] == (byte) 0xFF
                    && bytes[1] == (byte) 0xD8
                    && bytes[2] == (byte) 0xFF;

            boolean isPng = bytes.length > 4
                    && bytes[0] == (byte) 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47;

            String extension;
            if (isJpg) {
                extension = ".jpg";
            } else if (isPng) {
                extension = ".png";
            } else {
                throw new BadRequestException("Only JPG and PNG images are allowed");
            }

            Files.createDirectories(rootLocation);

            String uniqueFileName = UUID.randomUUID() + extension;
            Path destinationFile = rootLocation.resolve(uniqueFileName).normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(rootLocation)) {
                throw new BadRequestException("Cannot store file outside current directory");
            }

            Files.write(destinationFile, bytes);
            return uniqueFileName;

        } catch (IOException e) {
            throw new BadRequestException("Failed to store file");
        }
    }

}
