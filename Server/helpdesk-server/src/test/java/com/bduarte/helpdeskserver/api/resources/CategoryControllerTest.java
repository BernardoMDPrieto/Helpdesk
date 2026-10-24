package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.CategoryFilter;
import com.bduarte.helpdeskserver.api.requests.CreateCategoryDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateCategoryDTO;
import com.bduarte.helpdeskserver.api.responses.CategoryResponse;
import com.bduarte.helpdeskserver.services.CategoryServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.bduarte.helpdeskserver.support.TestFixtures.standaloneMockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryServices categoryServices;

    @InjectMocks
    private CategoryController categoryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(categoryController);
    }

    @Test
    void createCategoryReturnsCreated() throws Exception {
        when(categoryServices.createCategory(new CreateCategoryDTO("Rede", 8))).thenReturn(new CategoryResponse(1L, "Rede", 8));

        mockMvc.perform(post("/config/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryName": "Rede", "responseTimeInHoursDefault": 8}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryName").value("Rede"))
                .andExpect(jsonPath("$.responseTimeInHoursDefault").value(8));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"responseTimeInHoursDefault\": 8}",
            "{\"categoryName\": \" \", \"responseTimeInHoursDefault\": 8}",
            "{\"categoryName\": \"Rede\", \"responseTimeInHoursDefault\": 0}"
    })
    void createCategoryWithInvalidBodyReturnsBadRequest(String body) throws Exception {
        mockMvc.perform(post("/config/category").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryServices);
    }

    @Test
    void createCategoryPropagatesConflict() throws Exception {
        when(categoryServices.createCategory(any(CreateCategoryDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Categoria já cadastrada"));

        mockMvc.perform(post("/config/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryName": "Rede", "responseTimeInHoursDefault": 8}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getCategoriesUsesDefaultPagingAndBindsFilter() throws Exception {
        when(categoryServices.getCategories(any(CategoryFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new CategoryResponse(1L, "Rede", 8)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/config/category").param("categoryName", "re"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryName").value("Rede"));

        ArgumentCaptor<CategoryFilter> filter = ArgumentCaptor.forClass(CategoryFilter.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(categoryServices).getCategories(filter.capture(), pageable.capture());
        assertThat(filter.getValue().getCategoryName()).isEqualTo("re");
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "categoryName"));
    }

    @Test
    void getCategoryReturnsCategory() throws Exception {
        when(categoryServices.getCategoryById(1L)).thenReturn(new CategoryResponse(1L, "Rede", 8));

        mockMvc.perform(get("/config/category/{categoryId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Rede"));
    }

    @Test
    void getCategoryPropagatesNotFound() throws Exception {
        when(categoryServices.getCategoryById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));

        mockMvc.perform(get("/config/category/{categoryId}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCategoryReturnsNoContent() throws Exception {
        mockMvc.perform(put("/config/category/{categoryId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryName": "Redes", "responseTimeInHoursDefault": 4}
                                """))
                .andExpect(status().isNoContent());

        verify(categoryServices).updateCategory(new UpdateCategoryDTO("Redes", 4), 1L);
    }

    @Test
    void updateCategoryWithInvalidTimeReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/config/category/{categoryId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"responseTimeInHoursDefault": -1}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryServices);
    }

    @Test
    void deleteCategoryReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/config/category/{categoryId}", 1L))
                .andExpect(status().isNoContent());

        verify(categoryServices).deleteCategory(1L);
    }

    @Test
    void deleteCategoryPropagatesConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Categoria está em uso e não pode ser removida"))
                .when(categoryServices).deleteCategory(eq(1L));

        mockMvc.perform(delete("/config/category/{categoryId}", 1L))
                .andExpect(status().isConflict());
    }
}
