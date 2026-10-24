package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.SlaConfigFilter;
import com.bduarte.helpdeskserver.api.requests.CreateSlaConfigDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateSlaConfigDTO;
import com.bduarte.helpdeskserver.api.responses.SlaConfigResponse;
import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.models.Category;
import com.bduarte.helpdeskserver.models.SlaConfig;
import com.bduarte.helpdeskserver.repositories.CategoryRepository;
import com.bduarte.helpdeskserver.repositories.SlaConfigRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaConfigServicesTest {

    @Mock
    private SlaConfigRepository slaConfigRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SlaConfigServices slaConfigServices;

    private final Category category = category(1L, "Rede", 8);

    @Nested
    class CreateSlaConfig {

        @Test
        void savesNewSlaConfig() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(slaConfigRepository.existsByCategoryIdAndPriority(1L, Priority.HIGH)).thenReturn(false);
            when(slaConfigRepository.save(any(SlaConfig.class))).thenAnswer(invocation -> {
                SlaConfig slaConfig = invocation.getArgument(0);
                slaConfig.setId(10L);
                return slaConfig;
            });

            SlaConfigResponse response = slaConfigServices.createSlaConfig(new CreateSlaConfigDTO(1L, Priority.HIGH, 4, 24));

            ArgumentCaptor<SlaConfig> captor = ArgumentCaptor.forClass(SlaConfig.class);
            verify(slaConfigRepository).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isSameAs(category);
            assertThat(captor.getValue().getPriority()).isEqualTo(Priority.HIGH);
            assertThat(captor.getValue().getResponseTimeInHours()).isEqualTo(4);
            assertThat(captor.getValue().getResolutionTimeInHours()).isEqualTo(24);

            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getCategoryId()).isEqualTo(1L);
            assertThat(response.getCategoryName()).isEqualTo("Rede");
            assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(response.getResponseTimeInHours()).isEqualTo(4);
            assertThat(response.getResolutionTimeInHours()).isEqualTo(24);
        }

        @Test
        void unknownCategoryReturnsNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> slaConfigServices.createSlaConfig(new CreateSlaConfigDTO(99L, Priority.HIGH, 4, 24)));

            verify(slaConfigRepository, never()).save(any());
        }

        @Test
        void duplicateCategoryAndPriorityReturnsConflict() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(slaConfigRepository.existsByCategoryIdAndPriority(1L, Priority.HIGH)).thenReturn(true);

            assertThrowsStatus(HttpStatus.CONFLICT, () -> slaConfigServices.createSlaConfig(new CreateSlaConfigDTO(1L, Priority.HIGH, 4, 24)));

            verify(slaConfigRepository, never()).save(any());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void findSlaByCategoryDelegatesToRepository(boolean exists) {
        when(slaConfigRepository.existsByCategoryId(1L)).thenReturn(exists);

        assertThat(slaConfigServices.findSLAByCategory(1L)).isEqualTo(exists);
    }

    @Test
    void getSlaConfigsMapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(slaConfigRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(slaConfig(Priority.HIGH, 4, 24)), pageable, 1));

        Page<SlaConfigResponse> page = slaConfigServices.getSlaConfigs(new SlaConfigFilter(), pageable);

        assertThat(page.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getCategoryName()).isEqualTo("Rede");
            assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        });
    }

    @Nested
    class GetSlaConfigById {

        @Test
        void returnsSlaConfig() {
            when(slaConfigRepository.findById(10L)).thenReturn(Optional.of(slaConfig(Priority.LOW, 24, 72)));

            SlaConfigResponse response = slaConfigServices.getSlaConfigById(10L);

            assertThat(response.getPriority()).isEqualTo(Priority.LOW);
            assertThat(response.getResponseTimeInHours()).isEqualTo(24);
        }

        @Test
        void unknownSlaConfigReturnsNotFound() {
            when(slaConfigRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> slaConfigServices.getSlaConfigById(99L));
        }
    }

    @Nested
    class UpdateSlaConfig {

        @Test
        void updatesBothTimes() {
            SlaConfig slaConfig = slaConfig(Priority.HIGH, 4, 24);
            when(slaConfigRepository.findById(10L)).thenReturn(Optional.of(slaConfig));

            slaConfigServices.updateSlaConfig(new UpdateSlaConfigDTO(2, 12), 10L);

            assertThat(slaConfig.getResponseTimeInHours()).isEqualTo(2);
            assertThat(slaConfig.getResolutionTimeInHours()).isEqualTo(12);
            verify(slaConfigRepository).save(slaConfig);
        }

        @Test
        void nullTimesAreKept() {
            SlaConfig slaConfig = slaConfig(Priority.HIGH, 4, 24);
            when(slaConfigRepository.findById(10L)).thenReturn(Optional.of(slaConfig));

            slaConfigServices.updateSlaConfig(new UpdateSlaConfigDTO(null, null), 10L);

            assertThat(slaConfig.getResponseTimeInHours()).isEqualTo(4);
            assertThat(slaConfig.getResolutionTimeInHours()).isEqualTo(24);
        }

        @Test
        void unknownSlaConfigReturnsNotFound() {
            when(slaConfigRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> slaConfigServices.updateSlaConfig(new UpdateSlaConfigDTO(2, 12), 99L));

            verify(slaConfigRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteSlaConfig {

        @Test
        void deletesSlaConfig() {
            SlaConfig slaConfig = slaConfig(Priority.HIGH, 4, 24);
            when(slaConfigRepository.findById(10L)).thenReturn(Optional.of(slaConfig));

            slaConfigServices.deleteSlaConfig(10L);

            verify(slaConfigRepository).delete(slaConfig);
            verify(slaConfigRepository).flush();
        }

        @Test
        void unknownSlaConfigReturnsNotFound() {
            when(slaConfigRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> slaConfigServices.deleteSlaConfig(99L));

            verify(slaConfigRepository, never()).delete(any(SlaConfig.class));
        }

        @Test
        void slaConfigInUseReturnsConflict() {
            when(slaConfigRepository.findById(10L)).thenReturn(Optional.of(slaConfig(Priority.HIGH, 4, 24)));
            doThrow(new DataIntegrityViolationException("fk")).when(slaConfigRepository).flush();

            assertThrowsStatus(HttpStatus.CONFLICT, () -> slaConfigServices.deleteSlaConfig(10L));
        }
    }

    private SlaConfig slaConfig(Priority priority, int responseHours, int resolutionHours) {
        SlaConfig slaConfig = new SlaConfig();
        slaConfig.setId(10L);
        slaConfig.setCategory(category);
        slaConfig.setPriority(priority);
        slaConfig.setResponseTimeInHours(responseHours);
        slaConfig.setResolutionTimeInHours(resolutionHours);
        return slaConfig;
    }
}
