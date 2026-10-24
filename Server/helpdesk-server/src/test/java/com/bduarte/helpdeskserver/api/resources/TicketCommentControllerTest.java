package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.requests.CreateTicketCommentDTO;
import com.bduarte.helpdeskserver.api.responses.TicketCommentResponse;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.services.TicketCommentServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketCommentControllerTest {

    @Mock
    private TicketCommentServices ticketCommentServices;

    @InjectMocks
    private TicketCommentController ticketCommentController;

    private MockMvc mockMvc;
    private User currentUser;
    private UUID ticketId;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(ticketCommentController);

        currentUser = user(Role.RoleName.ROLE_AGENT);
        ticketId = UUID.randomUUID();

        authenticate(currentUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCommentReturnsCreated() throws Exception {
        TicketCommentResponse response = response("Verificar roteador", true);
        when(ticketCommentServices.createComment(eq(ticketId), any(CreateTicketCommentDTO.class), eq(currentUser)))
                .thenReturn(response);

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Verificar roteador", "internal": true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(response.getCommentId().toString()))
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.content").value("Verificar roteador"))
                .andExpect(jsonPath("$.internal").value(true));

        ArgumentCaptor<CreateTicketCommentDTO> captor = ArgumentCaptor.forClass(CreateTicketCommentDTO.class);
        verify(ticketCommentServices).createComment(eq(ticketId), captor.capture(), eq(currentUser));
        assertThat(captor.getValue().getContent()).isEqualTo("Verificar roteador");
        assertThat(captor.getValue().getInternal()).isTrue();
    }

    @Test
    void createCommentWithBlankContentReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "   "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ticketCommentServices);
    }

    @Test
    void createCommentPropagatesForbidden() throws Exception {
        when(ticketCommentServices.createComment(eq(ticketId), any(CreateTicketCommentDTO.class), eq(currentUser)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Apenas administradores e agentes podem criar comentários internos"));

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Nota", "internal": true}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCommentsUsesOldestFirstByDefault() throws Exception {
        TicketCommentResponse response = response("Ainda sem internet", false);
        when(ticketCommentServices.getComments(eq(ticketId), any(Pageable.class), eq(currentUser)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentId").value(response.getCommentId().toString()))
                .andExpect(jsonPath("$.content[0].content").value("Ainda sem internet"))
                .andExpect(jsonPath("$.content[0].internal").value(false));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketCommentServices).getComments(eq(ticketId), captor.capture(), eq(currentUser));
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    @Test
    void getCommentsHonoursPagingParameters() throws Exception {
        when(ticketCommentServices.getComments(eq(ticketId), any(Pageable.class), eq(currentUser)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId)
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketCommentServices).getComments(eq(ticketId), captor.capture(), eq(currentUser));
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void getCommentsPropagatesNotFound() throws Exception {
        when(ticketCommentServices.getComments(eq(ticketId), any(Pageable.class), eq(currentUser)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isNotFound());
    }

    private TicketCommentResponse response(String content, boolean internal) {
        return new TicketCommentResponse(UUID.randomUUID(), ticketId, currentUser.getId(),
                currentUser.getUserName(), content, internal, LocalDateTime.now());
    }
}
