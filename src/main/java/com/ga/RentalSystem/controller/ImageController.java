package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.repository.ImageRepository;
import com.ga.RentalSystem.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "Car images")
public class ImageController {
    private final ImageService imageService;

    @GetMapping("/{imageId}")
    @Operation(summary = "Get a car image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long imageId) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageService.getImage(imageId));
    }
}
