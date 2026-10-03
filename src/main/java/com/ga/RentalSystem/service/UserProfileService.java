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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    public UserProfileResponse createProfile(UserProfileRequest request,
                                             Authentication authentication){
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        if(currentUser.getRoleEnum() != Role.CUSTOMER){
            throw new ForbiddenException("Only customers can create a user profile");
        }
        if (userProfileRepository.existsByUserId(currentUser.getId())) {
            throw new BadRequestException("Profile already exists for this user");
        }

        UserProfile profile = new UserProfile();
        profile.setUser(currentUser);

        UserProfile saved = userProfileRepository.save(profile);





    }

}
