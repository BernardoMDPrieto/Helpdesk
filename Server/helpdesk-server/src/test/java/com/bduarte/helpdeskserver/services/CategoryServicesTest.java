package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.CategoryFilter;
import com.bduarte.helpdeskserver.api.requests.CreateCategoryDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateCategoryDTO;
import com.bduarte.helpdeskserver.api.responses.CategoryResponse;
import com.bduarte.helpdeskserver.models.Category;
import com.bduarte.helpdeskserver.repositories.CategoryRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static com.bduarte.helpdeskserver.support.TestFixtures.assertThrowsStatus;
import static com.bduarte.helpdeskserver.support.TestFixtures.category;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServicesTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServices categoryServices;

    @Nested
    class CreateCategory {

        @Test
        void savesNewCategory() {
            when(categoryRepository.existsByCategoryNameIgnoreCase("Rede")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
                Category category = invocation.getArgument(0);
                category.setId(1L);
                return category;
            });

            CategoryResponse response = categoryServices.createCategory(new CreateCategoryDTO("Rede", 8));

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getCategoryName()).isEqualTo("Rede");
            assertThat(captor.getValue().getResponseTimeInHoursDefault()).isEqualTo(8);
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getCategoryName()).isEqualTo("Rede");
            assertThat(response.getResponseTimeInHoursDefault()).isEqualTo(8);
        }

        @Test
        void duplicateNameReturnsConflict() {
            when(categoryRepository.existsByCategoryNameIgnoreCase("rede")).thenReturn(true);

            assertThrowsStatus(HttpStatus.CONFLICT, () -> categoryServices.createCategory(new CreateCategoryDTO("rede", 8)));

            verify(categoryRepository, never()).save(any());
        }
    }

    @Test
    void getCategoriesMapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(category(1L, "Rede", 8), category(2L, "Hardware", 12)), pageable, 2));

        Page<CategoryResponse> page = categoryServices.getCategories(new CategoryFilter(), pageable);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(CategoryResponse::getId, CategoryResponse::getCategoryName, CategoryResponse::getResponseTimeInHoursDefault)
                .containsExactly(tuple(1L, "Rede", 8), tuple(2L, "Hardware", 12));
    }

    @Nested
    class GetCategoryById {

        @Test
        void returnsCategory() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Rede", 8)));

            CategoryResponse response = categoryServices.getCategoryById(1L);

            assertThat(response.getCategoryName()).isEqualTo("Rede");
        }

        @Test
        void unknownCategoryReturnsNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> categoryServices.getCategoryById(99L));
        }
    }

    @Nested
    class UpdateCategory {

        @Test
        void updatesNameAndResponseTime() {
            Category category = category(1L, "Rede", 8);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByCategoryNameIgnoreCaseAndIdNot("Redes", 1L)).thenReturn(false);

            categoryServices.updateCategory(new UpdateCategoryDTO("Redes", 4), 1L);

            assertThat(category.getCategoryName()).isEqualTo("Redes");
            assertThat(category.getResponseTimeInHoursDefault()).isEqualTo(4);
            verify(categoryRepository).save(category);
        }

        @Test
        void nullOrBlankFieldsAreIgnored() {
            Category category = category(1L, "Rede", 8);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            categoryServices.updateCategory(new UpdateCategoryDTO(" ", null), 1L);

            assertThat(category.getCategoryName()).isEqualTo("Rede");
            assertThat(category.getResponseTimeInHoursDefault()).isEqualTo(8);
            verify(categoryRepository, never()).existsByCategoryNameIgnoreCaseAndIdNot(anyString(), anyLong());
            verify(categoryRepository).save(category);
        }

        @Test
        void nameUsedByAnotherCategoryReturnsConflict() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Rede", 8)));
            when(categoryRepository.existsByCategoryNameIgnoreCaseAndIdNot("Hardware", 1L)).thenReturn(true);

            assertThrowsStatus(HttpStatus.CONFLICT, () -> categoryServices.updateCategory(new UpdateCategoryDTO("Hardware", null), 1L));

            verify(categoryRepository, never()).save(any());
        }

        @Test
        void unknownCategoryReturnsNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> categoryServices.updateCategory(new UpdateCategoryDTO("Rede", 8), 99L));
        }
    }

    @Nested
    class DeleteCategory {

        @Test
        void deletesCategory() {
            Category category = category(1L, "Rede", 8);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            categoryServices.deleteCategory(1L);

            verify(categoryRepository).delete(category);
            verify(categoryRepository).flush();
        }

        @Test
        void unknownCategoryReturnsNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> categoryServices.deleteCategory(99L));

            verify(categoryRepository, never()).delete(any(Category.class));
        }

        @Test
        void categoryInUseReturnsConflict() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Rede", 8)));
            doThrow(new DataIntegrityViolationException("fk_tickets_category")).when(categoryRepository).flush();

            assertThrowsStatus(HttpStatus.CONFLICT, () -> categoryServices.deleteCategory(1L));
        }
    }
}
