package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.CategoryFilter;
import com.bduarte.helpdeskserver.api.filters.CategorySpecification;
import com.bduarte.helpdeskserver.api.requests.CreateCategoryDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateCategoryDTO;
import com.bduarte.helpdeskserver.api.responses.CategoryResponse;
import com.bduarte.helpdeskserver.models.Category;
import com.bduarte.helpdeskserver.repositories.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CategoryServices {
    private static final Logger logger = LoggerFactory.getLogger(CategoryServices.class);
    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryDTO categoryDTO) {
        logger.info("Creating new category with name: {}", categoryDTO.getCategoryName());

        if (categoryRepository.existsByCategoryNameIgnoreCase(categoryDTO.getCategoryName())) {
            logger.warn("Attempted to create category with name that already exists: {}", categoryDTO.getCategoryName());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Categoria já cadastrada"
            );
        }

        Category category = new Category(
                categoryDTO.getCategoryName(),
                categoryDTO.getResponseTimeInHoursDefault()
        );
        Category savedCategory = categoryRepository.save(category);
        logger.info("Category saved successfully with ID: {}", savedCategory.getId());
        return convertCategoryResponse(savedCategory);
    }

    public Page<CategoryResponse> getCategories(CategoryFilter categoryFilter, Pageable pageable) {
        logger.info("Fetching categories with filter: {} and pageable: page={}, size={}", categoryFilter, pageable.getPageNumber(), pageable.getPageSize());
        Specification<Category> spec = CategorySpecification.bySpecification(categoryFilter);

        Page<CategoryResponse> categories = categoryRepository.findAll(spec, pageable).map(this::convertCategoryResponse);
        logger.debug("Retrieved {} categories from database", categories.getTotalElements());
        return categories;
    }

    public CategoryResponse getCategoryById(Long id) {
        logger.info("Fetching category with ID: {}", id);
        return convertCategoryResponse(findCategory(id));
    }

    @Transactional
    public void updateCategory(UpdateCategoryDTO updateCategoryDTO, Long id) {
        logger.info("Updating category with ID: {}", id);
        Category existingCategory = findCategory(id);

        if (updateCategoryDTO.getCategoryName() != null && !updateCategoryDTO.getCategoryName().isBlank()) {
            if (categoryRepository.existsByCategoryNameIgnoreCaseAndIdNot(updateCategoryDTO.getCategoryName(), id)) {
                logger.warn("Attempted to rename category ID: {} to existing name: {}", id, updateCategoryDTO.getCategoryName());
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Categoria já cadastrada"
                );
            }
            existingCategory.setCategoryName(updateCategoryDTO.getCategoryName());
            logger.debug("Updated name for category ID: {}", id);
        }

        if (updateCategoryDTO.getResponseTimeInHoursDefault() != null) {
            existingCategory.setResponseTimeInHoursDefault(updateCategoryDTO.getResponseTimeInHoursDefault());
            logger.debug("Updated response time to: {} for category ID: {}", updateCategoryDTO.getResponseTimeInHoursDefault(), id);
        }

        categoryRepository.save(existingCategory);
        logger.info("Category ID: {} updated successfully", id);
    }

    @Transactional
    public void deleteCategory(Long id) {
        logger.info("Deleting category with ID: {}", id);
        Category existingCategory = findCategory(id);

        try {
            categoryRepository.delete(existingCategory);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            logger.warn("Category ID: {} is in use and cannot be deleted", id);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Categoria está em uso e não pode ser removida"
            );
        }
        logger.info("Category ID: {} deleted successfully", id);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Category not found with ID: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Categoria não encontrada"
                    );
                });
    }

    private CategoryResponse convertCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getCategoryName(),
                category.getResponseTimeInHoursDefault());
    }
}
