package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.MakeRequest;
import com.ga.RentalSystem.dto.response.MakeResponse;
import com.ga.RentalSystem.service.MakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/makes")
@RequiredArgsConstructor
@Tag(name = "Makes", description = "Car makes")
public class MakeController {

    private final MakeService makeService;

    @GetMapping
    @Operation(summary = "Get all makes")
    public List<MakeResponse> getAllMakes() {
        return makeService.getAllMakes();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a make by id")
    public MakeResponse getMakeById(@PathVariable Long id) {
        return makeService.getMakeById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a make")
    public ResponseEntity<MakeResponse> createMake(@RequestBody @Valid MakeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(makeService.saveMake(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a make")
    public MakeResponse updateMake(@PathVariable Long id, @RequestBody @Valid MakeRequest request) {
        return makeService.updateMake(id, request);
    }


}