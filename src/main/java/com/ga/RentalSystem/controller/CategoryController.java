package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.CategoryRequest;
import com.ga.RentalSystem.dto.response.CategoryResponse;
import com.ga.RentalSystem.service.CategoryService;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Car categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories")
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a category by id")
    public CategoryResponse getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.saveCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category")
    public CategoryResponse updateCategory(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        return categoryService.updateCategory(id, request);
    }


}