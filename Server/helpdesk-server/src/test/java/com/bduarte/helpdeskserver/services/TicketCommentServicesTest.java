package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.requests.CreateTicketCommentDTO;
import com.bduarte.helpdeskserver.api.responses.TicketCommentResponse;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.Ticket;
import com.bduarte.helpdeskserver.models.TicketComment;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.repositories.TicketCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.bduarte.helpdeskserver.support.TestFixtures.assertThrowsStatus;
import static com.bduarte.helpdeskserver.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCommentServicesTest {

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private TicketServices ticketServices;

    @InjectMocks
    private TicketCommentServices ticketCommentServices;

    private UUID ticketId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        ticket = new Ticket();
        ticket.setTicketId(ticketId);
    }

    @Nested
    class CreateComment {

        @BeforeEach
        void stubSave() {
            // Mimics JPA: the saved entity gets an id and its @PrePersist timestamp
            lenient().when(ticketCommentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> {
                TicketComment comment = invocation.getArgument(0);
                comment.setId(UUID.randomUUID());
                comment.prePersist();
                return comment;
            });
        }

        @Test
        void customerCreatesPublicComment() {
            User customer = user(Role.RoleName.ROLE_CUSTOMER);
            when(ticketServices.findAccessibleTicket(ticketId, customer)).thenReturn(ticket);

            TicketCommentResponse response = ticketCommentServices.createComment(
                    ticketId, new CreateTicketCommentDTO("Ainda sem internet", false), customer);

            TicketComment saved = captureSavedComment();
            assertThat(saved.getTicket()).isSameAs(ticket);
            assertThat(saved.getUser()).isSameAs(customer);
            assertThat(saved.getContent()).isEqualTo("Ainda sem internet");
            assertThat(saved.isInternal()).isFalse();

            assertThat(response.getCommentId()).isEqualTo(saved.getId());
            assertThat(response.getTicketId()).isEqualTo(ticketId);
            assertThat(response.getAuthorId()).isEqualTo(customer.getId());
            assertThat(response.getAuthorName()).isEqualTo(customer.getUserName());
            assertThat(response.getContent()).isEqualTo("Ainda sem internet");
            assertThat(response.isInternal()).isFalse();
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        void missingInternalFlagDefaultsToPublic() {
            User customer = user(Role.RoleName.ROLE_CUSTOMER);
            when(ticketServices.findAccessibleTicket(ticketId, customer)).thenReturn(ticket);

            TicketCommentResponse response = ticketCommentServices.createComment(
                    ticketId, new CreateTicketCommentDTO("Olá", null), customer);

            assertThat(captureSavedComment().isInternal()).isFalse();
            assertThat(response.isInternal()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = Role.RoleName.class, names = {"ROLE_ADMIN", "ROLE_AGENT"})
        void staffCanCreateInternalComment(Role.RoleName roleName) {
            User staff = user(roleName);
            when(ticketServices.findAccessibleTicket(ticketId, staff)).thenReturn(ticket);

            TicketCommentResponse response = ticketCommentServices.createComment(
                    ticketId, new CreateTicketCommentDTO("Nota interna", true), staff);

            assertThat(captureSavedComment().isInternal()).isTrue();
            assertThat(response.isInternal()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = Role.RoleName.class, names = {"ROLE_CUSTOMER", "ROLE_VIEWER"})
        void nonStaffCannotCreateInternalComment(Role.RoleName roleName) {
            User user = user(roleName);
            when(ticketServices.findAccessibleTicket(ticketId, user)).thenReturn(ticket);

            assertThrowsStatus(HttpStatus.FORBIDDEN, () -> ticketCommentServices.createComment(
                    ticketId, new CreateTicketCommentDTO("Nota interna", true), user));

            verify(ticketCommentRepository, never()).save(any());
        }

        @Test
        void inaccessibleTicketIsNotCommented() {
            User customer = user(Role.RoleName.ROLE_CUSTOMER);
            when(ticketServices.findAccessibleTicket(ticketId, customer))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketCommentServices.createComment(
                    ticketId, new CreateTicketCommentDTO("Olá", false), customer));

            verify(ticketCommentRepository, never()).save(any());
        }

        private TicketComment captureSavedComment() {
            ArgumentCaptor<TicketComment> captor = ArgumentCaptor.forClass(TicketComment.class);
            verify(ticketCommentRepository).save(captor.capture());
            return captor.getValue();
        }
    }

    @Nested
    class GetComments {

        private final Pageable pageable = PageRequest.of(0, 20);

        @ParameterizedTest
        @EnumSource(value = Role.RoleName.class, names = {"ROLE_ADMIN", "ROLE_AGENT"})
        void staffSeesInternalComments(Role.RoleName roleName) {
            User staff = user(roleName);
            User customer = user(Role.RoleName.ROLE_CUSTOMER);
            TicketComment publicComment = comment(customer, "Ainda sem internet", false);
            TicketComment internalComment = comment(staff, "Verificar roteador", true);
            when(ticketServices.findAccessibleTicket(ticketId, staff)).thenReturn(ticket);
            when(ticketCommentRepository.findByTicketTicketId(ticketId, pageable))
                    .thenReturn(new PageImpl<>(List.of(publicComment, internalComment), pageable, 2));

            Page<TicketCommentResponse> page = ticketCommentServices.getComments(ticketId, pageable, staff);

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(TicketCommentResponse::getContent, TicketCommentResponse::isInternal)
                    .containsExactly(
                            tuple("Ainda sem internet", false),
                            tuple("Verificar roteador", true));
            verify(ticketCommentRepository, never()).findByTicketTicketIdAndIsInternalFalse(any(), any());
        }

        @Test
        void customerOnlySeesPublicComments() {
            User customer = user(Role.RoleName.ROLE_CUSTOMER);
            TicketComment publicComment = comment(customer, "Ainda sem internet", false);
            when(ticketServices.findAccessibleTicket(ticketId, customer)).thenReturn(ticket);
            when(ticketCommentRepository.findByTicketTicketIdAndIsInternalFalse(ticketId, pageable))
                    .thenReturn(new PageImpl<>(List.of(publicComment), pageable, 1));

            Page<TicketCommentResponse> page = ticketCommentServices.getComments(ticketId, pageable, customer);

            assertThat(page.getContent()).singleElement().satisfies(response -> {
                assertThat(response.getCommentId()).isEqualTo(publicComment.getId());
                assertThat(response.getTicketId()).isEqualTo(ticketId);
                assertThat(response.getAuthorId()).isEqualTo(customer.getId());
                assertThat(response.getAuthorName()).isEqualTo(customer.getUserName());
                assertThat(response.isInternal()).isFalse();
                assertThat(response.getCreatedAt()).isEqualTo(publicComment.getCreatedAt());
            });
            verify(ticketCommentRepository, never()).findByTicketTicketId(any(), any());
        }

        @Test
        void inaccessibleTicketReturnsNoComments() {
            User customer = user(Role.RoleName.ROLE_CUSTOMER);
            when(ticketServices.findAccessibleTicket(ticketId, customer))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketCommentServices.getComments(ticketId, pageable, customer));

            verifyNoInteractions(ticketCommentRepository);
        }

        private TicketComment comment(User author, String content, boolean internal) {
            TicketComment comment = new TicketComment();
            comment.setId(UUID.randomUUID());
            comment.setTicket(ticket);
            comment.setUser(author);
            comment.setContent(content);
            comment.setInternal(internal);
            comment.setCreatedAt(LocalDateTime.now());
            return comment;
        }
    }

}
