package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.CategoryFilter;
import com.bduarte.helpdeskserver.api.requests.CreateCategoryDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateCategoryDTO;
import com.bduarte.helpdeskserver.api.responses.CategoryResponse;
import com.bduarte.helpdeskserver.services.CategoryServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class CategoryController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryServices categoryServices;

    @PostMapping("/category")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryDTO categoryDTO) {
        logger.info("Create category request received for name: {}", categoryDTO.getCategoryName());
        CategoryResponse category = categoryServices.createCategory(categoryDTO);
        logger.info("Category created successfully with ID: {}", category.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @GetMapping("/category")
    public ResponseEntity<Page<CategoryResponse>> getCategories(@ModelAttribute CategoryFilter categoryFilter,
                                                                @PageableDefault(page = 0, size = 10, sort = "categoryName", direction = Sort.Direction.ASC) Pageable pageable) {
        logger.info("Fetching categories with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<CategoryResponse> categories = categoryServices.getCategories(categoryFilter, pageable);
        logger.debug("Categories retrieved successfully, total count: {}", categories.getTotalElements());
        return ResponseEntity.status(HttpStatus.OK).body(categories);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long categoryId) {
        logger.info("Fetching category with ID: {}", categoryId);
        return ResponseEntity.status(HttpStatus.OK).body(categoryServices.getCategoryById(categoryId));
    }

    @PutMapping("/category/{categoryId}")
    public ResponseEntity<Void> updateCategory(@Valid @RequestBody UpdateCategoryDTO categoryDTO, @PathVariable Long categoryId) {
        logger.info("Update category request received for category ID: {}", categoryId);
        categoryServices.updateCategory(categoryDTO, categoryId);
        logger.info("Category updated successfully with ID: {}", categoryId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/category/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        logger.info("Delete category request received for category ID: {}", categoryId);
        categoryServices.deleteCategory(categoryId);
        logger.info("Category deleted successfully with ID: {}", categoryId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
