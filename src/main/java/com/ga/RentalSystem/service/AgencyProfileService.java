package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.AgencyProfileRequest;
import com.ga.RentalSystem.dto.response.AgencyProfileResponse;
import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ForbiddenException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.AgencyProfile;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.model.UserProfile;
import com.ga.RentalSystem.repository.AgencyProfileRepository;
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
public class AgencyProfileService {

    private final AgencyProfileRepository agencyProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

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

        AgencyProfile saved = agencyProfileRepository.save(profile);

        String message = "Agency " + currentUser.getId() + " created their profile";
        log.info(message);
        auditLogService.log(currentUser.getId(), "PROFILE_CREATED", "AgencyProfile", saved.getId(), message);

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

        AgencyProfile updated = agencyProfileRepository.save(profile);

        String message = "Agency " + currentUser.getId() + " updated their profile";
        log.info(message);
        auditLogService.log(currentUser.getId(),
                "PROFILE_UPDATED", "AgencyProfile", updated.getId(), message);

        return toResponse(updated);
    }

    public byte[] getAgencyProfilePicture(Authentication authentication){
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        AgencyProfile profile = agencyProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new InformationNotFoundException("Profile not found"));

        if (profile.getLogoUrl() == null) {
            throw new InformationNotFoundException("No profile picture uploaded");
        }

        Path file = rootLocation.resolve(profile.getLogoUrl()).normalize();

        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new InformationNotFoundException("Profile picture file not found");
        }
    }

    public void uploadLogo(MultipartFile file, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        AgencyProfile profile = agencyProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new InformationNotFoundException("Create your profile first"));

        profile.setLogoUrl(saveFile(file));
        agencyProfileRepository.save(profile);

        String message = "Agency " + currentUser.getId() + " uploaded a logo";
        log.info(message);
        auditLogService.log(currentUser.getId(), "PROFILE_UPDATED", "AgencyProfile", profile.getId(), message);
    }

    private final Path rootLocation = Paths.get("uploads/agency").toAbsolutePath().normalize();

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