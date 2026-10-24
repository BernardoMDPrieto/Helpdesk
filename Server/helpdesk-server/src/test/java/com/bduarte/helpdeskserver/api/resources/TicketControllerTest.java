package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.TicketFilter;
import com.bduarte.helpdeskserver.api.requests.CreateTicketDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateTicketDTO;
import com.bduarte.helpdeskserver.api.responses.TicketResponse;
import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.services.TicketServices;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.bduarte.helpdeskserver.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketServices ticketServices;

    @InjectMocks
    private TicketController ticketController;

    private MockMvc mockMvc;
    private User currentUser;
    private UUID ticketId;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(ticketController);
        currentUser = user(Role.RoleName.ROLE_CUSTOMER);
        authenticate(currentUser);
        ticketId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTicketReturnsCreated() throws Exception {
        when(ticketServices.createTicket(any(CreateTicketDTO.class), eq(currentUser))).thenReturn(response());

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Sem internet", "description": "Caiu tudo", "categoryId": 1, "priority": "HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        ArgumentCaptor<CreateTicketDTO> captor = ArgumentCaptor.forClass(CreateTicketDTO.class);
        verify(ticketServices).createTicket(captor.capture(), eq(currentUser));
        assertThat(captor.getValue()).isEqualTo(new CreateTicketDTO("Sem internet", "Caiu tudo", 1L, Priority.HIGH));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"description\": \"Caiu tudo\", \"categoryId\": 1, \"priority\": \"HIGH\"}",
            "{\"title\": \"Sem internet\", \"description\": \" \", \"categoryId\": 1, \"priority\": \"HIGH\"}",
            "{\"title\": \"Sem internet\", \"description\": \"Caiu tudo\", \"priority\": \"HIGH\"}",
            "{\"title\": \"Sem internet\", \"description\": \"Caiu tudo\", \"categoryId\": 1}"
    })
    void createTicketWithInvalidBodyReturnsBadRequest(String body) throws Exception {
        mockMvc.perform(post("/tickets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ticketServices);
    }

    @Test
    void getTicketsUsesDefaultPagingAndBindsFilter() throws Exception {
        UUID agentId = UUID.randomUUID();
        when(ticketServices.getTickets(any(TicketFilter.class), any(Pageable.class), eq(currentUser)))
                .thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/tickets")
                        .param("title", "internet")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("categoryId", "1")
                        .param("agentId", agentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticketId").value(ticketId.toString()));

        ArgumentCaptor<TicketFilter> filter = ArgumentCaptor.forClass(TicketFilter.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketServices).getTickets(filter.capture(), pageable.capture(), eq(currentUser));
        assertThat(filter.getValue().getTitle()).isEqualTo("internet");
        assertThat(filter.getValue().getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(filter.getValue().getPriority()).isEqualTo(Priority.HIGH);
        assertThat(filter.getValue().getCategoryId()).isEqualTo(1L);
        assertThat(filter.getValue().getAgentId()).isEqualTo(agentId);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void getTicketReturnsTicket() throws Exception {
        when(ticketServices.getTicketById(ticketId, currentUser)).thenReturn(response());

        mockMvc.perform(get("/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.title").value("Sem internet"))
                .andExpect(jsonPath("$.categoryName").value("Rede"));
    }

    @Test
    void getTicketPropagatesNotFound() throws Exception {
        when(ticketServices.getTicketById(ticketId, currentUser))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));

        mockMvc.perform(get("/tickets/{ticketId}", ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTicketReturnsNoContent() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Novo título", "status": "IN_PROGRESS"}
                                """))
                .andExpect(status().isNoContent());

        ArgumentCaptor<UpdateTicketDTO> captor = ArgumentCaptor.forClass(UpdateTicketDTO.class);
        verify(ticketServices).updateTicket(captor.capture(), eq(ticketId), eq(currentUser));
        assertThat(captor.getValue().getTitle()).isEqualTo("Novo título");
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void updateTicketPropagatesForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cliente só pode alterar o título e a descrição do ticket"))
                .when(ticketServices).updateTicket(any(UpdateTicketDTO.class), eq(ticketId), eq(currentUser));

        mockMvc.perform(put("/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CLOSED"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTicketReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/tickets/{ticketId}", ticketId))
                .andExpect(status().isNoContent());

        verify(ticketServices).deleteTicket(ticketId);
    }

    @Test
    void deleteTicketPropagatesConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ticket possui registros vinculados e não pode ser removido"))
                .when(ticketServices).deleteTicket(ticketId);

        mockMvc.perform(delete("/tickets/{ticketId}", ticketId))
                .andExpect(status().isConflict());
    }

    private TicketResponse response() {
        LocalDateTime now = LocalDateTime.now();
        return new TicketResponse(ticketId, "Sem internet", "Caiu tudo", currentUser.getId(), currentUser.getUserName(),
                null, null, TicketStatus.OPEN, 1L, "Rede", Priority.HIGH, false,
                now.plusHours(4), now.plusHours(24), false, false, now, now, null);
    }
}
