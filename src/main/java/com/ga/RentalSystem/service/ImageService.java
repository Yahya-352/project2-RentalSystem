package com.ga.RentalSystem.service;

import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.Image;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.model.UserProfile;
import com.ga.RentalSystem.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final Path rootLocation = Paths.get("uploads/cars").toAbsolutePath().normalize();

    public byte[] getImage(Long imageId){
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new InformationNotFoundException("Image not found"));

        Path file = rootLocation.resolve((image.getFileName())).normalize();
        try{
            return Files.readAllBytes(file);
        }catch (Exception e){
            throw new InformationNotFoundException("Image not found");
        }
    }
}

//
//public byte[] getUserProfilePicture(Authentication authentication){
//    User user = userRepository.findByEmail(authentication.getName())
//            .orElseThrow(() -> new InformationNotFoundException("User not found"));
//
//    UserProfile profile = userProfileRepository.findByUserId(user.getId())
//            .orElseThrow(() -> new InformationNotFoundException("Profile not found"));
//
//    if (profile.getProfilePictureUrl() == null) {
//        throw new InformationNotFoundException("No profile picture uploaded");
//    }
//
//    Path file = rootLocation.resolve(profile.getProfilePictureUrl()).normalize();
//
//    try {
//        return Files.readAllBytes(file);
//    } catch (IOException e) {
//        throw new InformationNotFoundException("Profile picture file not found");
//    }
//}
