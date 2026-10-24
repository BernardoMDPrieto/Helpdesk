package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.SlaConfigFilter;
import com.bduarte.helpdeskserver.api.requests.CreateSlaConfigDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateSlaConfigDTO;
import com.bduarte.helpdeskserver.api.responses.SlaConfigResponse;
import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.services.SlaConfigServices;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SlaConfigControllerTest {

    @Mock
    private SlaConfigServices slaConfigServices;

    @InjectMocks
    private SlaConfigController slaConfigController;

    private MockMvc mockMvc;

    private final SlaConfigResponse response = new SlaConfigResponse(10L, 1L, "Rede", Priority.HIGH, 4, 24);

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(slaConfigController);
    }

    @Test
    void createSlaConfigReturnsCreated() throws Exception {
        when(slaConfigServices.createSlaConfig(new CreateSlaConfigDTO(1L, Priority.HIGH, 4, 24))).thenReturn(response);

        mockMvc.perform(post("/config/sla")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "priority": "HIGH", "responseTimeInHours": 4, "resolutionTimeInHours": 24}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.categoryName").value("Rede"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.responseTimeInHours").value(4))
                .andExpect(jsonPath("$.resolutionTimeInHours").value(24));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"priority\": \"HIGH\", \"responseTimeInHours\": 4, \"resolutionTimeInHours\": 24}",
            "{\"categoryId\": 1, \"responseTimeInHours\": 4, \"resolutionTimeInHours\": 24}",
            "{\"categoryId\": 1, \"priority\": \"HIGH\", \"resolutionTimeInHours\": 24}",
            "{\"categoryId\": 1, \"priority\": \"HIGH\", \"responseTimeInHours\": 4}",
            "{\"categoryId\": 1, \"priority\": \"HIGH\", \"responseTimeInHours\": 0, \"resolutionTimeInHours\": 24}",
            "{\"categoryId\": 1, \"priority\": \"HIGH\", \"responseTimeInHours\": 4, \"resolutionTimeInHours\": -1}"
    })
    void createSlaConfigWithInvalidBodyReturnsBadRequest(String body) throws Exception {
        mockMvc.perform(post("/config/sla").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(slaConfigServices);
    }

    @Test
    void createSlaConfigPropagatesConflict() throws Exception {
        when(slaConfigServices.createSlaConfig(any(CreateSlaConfigDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma configuração de SLA para esta categoria e prioridade"));

        mockMvc.perform(post("/config/sla")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "priority": "HIGH", "responseTimeInHours": 4, "resolutionTimeInHours": 24}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getSlaConfigsUsesDefaultPagingAndBindsFilter() throws Exception {
        when(slaConfigServices.getSlaConfigs(any(SlaConfigFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/config/sla").param("categoryId", "1").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));

        ArgumentCaptor<SlaConfigFilter> filter = ArgumentCaptor.forClass(SlaConfigFilter.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(slaConfigServices).getSlaConfigs(filter.capture(), pageable.capture());
        assertThat(filter.getValue().getCategoryId()).isEqualTo(1L);
        assertThat(filter.getValue().getPriority()).isEqualTo(Priority.HIGH);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void getSlaConfigReturnsSlaConfig() throws Exception {
        when(slaConfigServices.getSlaConfigById(10L)).thenReturn(response);

        mockMvc.perform(get("/config/sla/{slaConfigId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void getSlaConfigPropagatesNotFound() throws Exception {
        when(slaConfigServices.getSlaConfigById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração de SLA não encontrada"));

        mockMvc.perform(get("/config/sla/{slaConfigId}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSlaConfigReturnsNoContent() throws Exception {
        mockMvc.perform(put("/config/sla/{slaConfigId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"responseTimeInHours": 2, "resolutionTimeInHours": 12}
                                """))
                .andExpect(status().isNoContent());

        verify(slaConfigServices).updateSlaConfig(new UpdateSlaConfigDTO(2, 12), 10L);
    }

    @Test
    void updateSlaConfigWithInvalidTimeReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/config/sla/{slaConfigId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resolutionTimeInHours": 0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(slaConfigServices);
    }

    @Test
    void deleteSlaConfigReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/config/sla/{slaConfigId}", 10L))
                .andExpect(status().isNoContent());

        verify(slaConfigServices).deleteSlaConfig(10L);
    }

    @Test
    void deleteSlaConfigPropagatesNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração de SLA não encontrada"))
                .when(slaConfigServices).deleteSlaConfig(99L);

        mockMvc.perform(delete("/config/sla/{slaConfigId}", 99L))
                .andExpect(status().isNotFound());
    }
}
