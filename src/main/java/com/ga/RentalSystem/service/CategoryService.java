package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.CategoryRequest;
import com.ga.RentalSystem.dto.response.CategoryResponse;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.Category;
import com.ga.RentalSystem.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> toResponse(category))
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        return toResponse(findCategory(id));
    }

    public CategoryResponse saveCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        return toResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        category.setName(request.name());
        return toResponse(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new InformationNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Category not found with id: " + id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}