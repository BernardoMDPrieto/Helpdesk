package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.TicketFilter;
import com.bduarte.helpdeskserver.api.requests.CreateTicketDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateTicketDTO;
import com.bduarte.helpdeskserver.api.responses.TicketResponse;
import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import com.bduarte.helpdeskserver.models.Category;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.SlaConfig;
import com.bduarte.helpdeskserver.models.Ticket;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.repositories.CategoryRepository;
import com.bduarte.helpdeskserver.repositories.SlaConfigRepository;
import com.bduarte.helpdeskserver.repositories.TicketRepository;
import com.bduarte.helpdeskserver.repositories.UserRepository;
import com.bduarte.helpdeskserver.support.CriteriaMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.bduarte.helpdeskserver.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServicesTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SlaConfigRepository slaConfigRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketServices ticketServices;

    private User customer;
    private User agent;
    private Category category;

    @BeforeEach
    void setUp() {
        customer = user(Role.RoleName.ROLE_CUSTOMER);
        agent = user(Role.RoleName.ROLE_AGENT);
        category = category(1L, "Rede", 8);
    }

    @Nested
    class CreateTicket {

        @BeforeEach
        void stubSave() {
            lenient().when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket ticket = invocation.getArgument(0);
                ticket.setTicketId(UUID.randomUUID());
                ticket.prePersist();
                return ticket;
            });
        }

        @Test
        void customerTicketStartsOpenWithUnconfirmedPriority() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(slaConfigRepository.findByCategoryIdAndPriority(1L, Priority.HIGH))
                    .thenReturn(Optional.of(slaConfig(category, Priority.HIGH, 4, 24)));

            LocalDateTime before = LocalDateTime.now();
            TicketResponse response = ticketServices.createTicket(
                    new CreateTicketDTO("Sem internet", "Caiu tudo", 1L, Priority.HIGH), customer);
            LocalDateTime after = LocalDateTime.now();

            Ticket saved = captureSavedTicket();
            assertThat(saved.getTitle()).isEqualTo("Sem internet");
            assertThat(saved.getDescription()).isEqualTo("Caiu tudo");
            assertThat(saved.getCustomer()).isSameAs(customer);
            assertThat(saved.getCategory()).isSameAs(category);
            assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(saved.isPriorityConfirmed()).isFalse();
            assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(saved.getAgent()).isNull();
            assertThat(saved.getSlaResponseDeadline()).isBetween(before.plusHours(4), after.plusHours(4));
            assertThat(saved.getSlaResolutionDeadline()).isBetween(before.plusHours(24), after.plusHours(24));

            assertThat(response.getTicketId()).isEqualTo(saved.getTicketId());
            assertThat(response.getCustomerId()).isEqualTo(customer.getId());
            assertThat(response.getCategoryName()).isEqualTo("Rede");
            assertThat(response.isPriorityConfirmed()).isFalse();
        }

        @Test
        void adminTicketHasConfirmedPriority() {
            User admin = user(Role.RoleName.ROLE_ADMIN);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(slaConfigRepository.findByCategoryIdAndPriority(1L, Priority.LOW)).thenReturn(Optional.empty());

            TicketResponse response = ticketServices.createTicket(
                    new CreateTicketDTO("Trocar mouse", "Mouse quebrado", 1L, Priority.LOW), admin);

            assertThat(captureSavedTicket().isPriorityConfirmed()).isTrue();
            assertThat(response.isPriorityConfirmed()).isTrue();
        }

        @Test
        void withoutSlaConfigUsesCategoryDefaultAndNoResolutionDeadline() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(slaConfigRepository.findByCategoryIdAndPriority(1L, Priority.MEDIUM)).thenReturn(Optional.empty());

            LocalDateTime before = LocalDateTime.now();
            ticketServices.createTicket(new CreateTicketDTO("Lento", "Rede lenta", 1L, Priority.MEDIUM), customer);
            LocalDateTime after = LocalDateTime.now();

            Ticket saved = captureSavedTicket();
            assertThat(saved.getSlaResponseDeadline()).isBetween(before.plusHours(8), after.plusHours(8));
            assertThat(saved.getSlaResolutionDeadline()).isNull();
        }

        @Test
        void unknownCategoryReturnsNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketServices.createTicket(
                    new CreateTicketDTO("Lento", "Rede lenta", 99L, Priority.MEDIUM), customer));

            verify(ticketRepository, never()).save(any());
        }

        private Ticket captureSavedTicket() {
            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            return captor.getValue();
        }
    }

    @Nested
    class GetTickets {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        void customerOnlySeesOwnTickets() {
            Ticket ticket = ticket(customer, category);
            when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(ticket), pageable, 1));

            Page<TicketResponse> page = ticketServices.getTickets(new TicketFilter(), pageable, customer);

            assertThat(page.getContent()).extracting(TicketResponse::getTicketId).containsExactly(ticket.getTicketId());
            CriteriaMocks<Ticket> criteria = applyCapturedSpec();
            verify(criteria.builder()).equal(criteria.path("customer.id"), customer.getId());
        }

        @ParameterizedTest
        @EnumSource(value = Role.RoleName.class, names = {"ROLE_ADMIN", "ROLE_AGENT"})
        void staffSeesAllTickets(Role.RoleName roleName) {
            when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            ticketServices.getTickets(new TicketFilter(), pageable, user(roleName));

            CriteriaMocks<Ticket> criteria = applyCapturedSpec();
            verify(criteria.builder(), never()).equal(any(), any(Object.class));
        }

        @Test
        void filterIsAppliedToQuery() {
            TicketFilter filter = new TicketFilter();
            filter.setStatus(TicketStatus.IN_PROGRESS);
            when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            ticketServices.getTickets(filter, pageable, agent);

            CriteriaMocks<Ticket> criteria = applyCapturedSpec();
            verify(criteria.builder()).equal(criteria.path("status"), TicketStatus.IN_PROGRESS);
        }

        @SuppressWarnings("unchecked")
        private CriteriaMocks<Ticket> applyCapturedSpec() {
            ArgumentCaptor<Specification<Ticket>> captor = ArgumentCaptor.forClass(Specification.class);
            verify(ticketRepository).findAll(captor.capture(), eq(pageable));
            CriteriaMocks<Ticket> criteria = new CriteriaMocks<>();
            criteria.apply(captor.getValue());
            return criteria;
        }
    }

    @Nested
    class GetTicketById {

        @Test
        void mapsEveryField() {
            Ticket ticket = ticket(customer, category);
            ticket.setAgent(agent);
            ticket.setPriorityConfirmed(true);
            ticket.setSlaResponseDeadline(ticket.getCreatedAt().plusHours(4));
            ticket.setSlaResolutionDeadline(ticket.getCreatedAt().plusHours(24));
            ticket.setSlaResponseBreached(true);
            ticket.setResolvedAt(ticket.getCreatedAt().plusHours(2));
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

            TicketResponse response = ticketServices.getTicketById(ticket.getTicketId(), customer);

            assertThat(response.getTicketId()).isEqualTo(ticket.getTicketId());
            assertThat(response.getTitle()).isEqualTo(ticket.getTitle());
            assertThat(response.getDescription()).isEqualTo(ticket.getDescription());
            assertThat(response.getCustomerId()).isEqualTo(customer.getId());
            assertThat(response.getCustomerName()).isEqualTo(customer.getUserName());
            assertThat(response.getAgentId()).isEqualTo(agent.getId());
            assertThat(response.getAgentName()).isEqualTo(agent.getUserName());
            assertThat(response.getStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(response.getCategoryId()).isEqualTo(1L);
            assertThat(response.getCategoryName()).isEqualTo("Rede");
            assertThat(response.getPriority()).isEqualTo(Priority.MEDIUM);
            assertThat(response.isPriorityConfirmed()).isTrue();
            assertThat(response.getSlaResponseDeadline()).isEqualTo(ticket.getSlaResponseDeadline());
            assertThat(response.getSlaResolutionDeadline()).isEqualTo(ticket.getSlaResolutionDeadline());
            assertThat(response.isSlaResponseBreached()).isTrue();
            assertThat(response.isSlaResolutionBreached()).isFalse();
            assertThat(response.getCreatedAt()).isEqualTo(ticket.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(ticket.getUpdatedAt());
            assertThat(response.getResolvedAt()).isEqualTo(ticket.getResolvedAt());
        }

        @Test
        void unassignedTicketHasNoAgent() {
            Ticket ticket = ticket(customer, category);
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

            TicketResponse response = ticketServices.getTicketById(ticket.getTicketId(), agent);

            assertThat(response.getAgentId()).isNull();
            assertThat(response.getAgentName()).isNull();
        }

        @Test
        void customerCannotSeeAnotherCustomersTicket() {
            Ticket ticket = ticket(user(Role.RoleName.ROLE_CUSTOMER, "other@helpdesk.com"), category);
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketServices.getTicketById(ticket.getTicketId(), customer));
        }

        @Test
        void unknownTicketReturnsNotFound() {
            UUID id = UUID.randomUUID();
            when(ticketRepository.findById(id)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketServices.getTicketById(id, agent));
        }
    }

    @Nested
    class UpdateTicket {

        private Ticket ticket;

        @BeforeEach
        void setUp() {
            ticket = ticket(customer, category);
            lenient().when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));
        }

        @Test
        void customerUpdatesTitleAndDescription() {
            ticketServices.updateTicket(update(dto -> {
                dto.setTitle("Sem internet no 2º andar");
                dto.setDescription("Só no segundo andar");
            }), ticket.getTicketId(), customer);

            verify(ticketRepository).save(ticket);
            assertThat(ticket.getTitle()).isEqualTo("Sem internet no 2º andar");
            assertThat(ticket.getDescription()).isEqualTo("Só no segundo andar");
        }

        @Test
        void blankTitleAndDescriptionAreIgnored() {
            ticketServices.updateTicket(update(dto -> {
                dto.setTitle("  ");
                dto.setDescription("");
            }), ticket.getTicketId(), customer);

            assertThat(ticket.getTitle()).isEqualTo("Sem internet");
            assertThat(ticket.getDescription()).isEqualTo("A internet caiu desde cedo");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.bduarte.helpdeskserver.services.TicketServicesTest#restrictedFields")
        void customerCannotChangeRestrictedFields(String field, UpdateTicketDTO dto) {
            assertThrowsStatus(HttpStatus.FORBIDDEN, () -> ticketServices.updateTicket(dto, ticket.getTicketId(), customer));

            verify(ticketRepository, never()).save(any());
        }

        @Test
        void customerCannotUpdateAnotherCustomersTicket() {
            User otherCustomer = user(Role.RoleName.ROLE_CUSTOMER, "other@helpdesk.com");

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketServices.updateTicket(
                    update(dto -> dto.setTitle("Novo")), ticket.getTicketId(), otherCustomer));

            verify(ticketRepository, never()).save(any());
        }

        @Test
        void unknownTicketReturnsNotFound() {
            UUID id = UUID.randomUUID();
            when(ticketRepository.findById(id)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketServices.updateTicket(update(dto -> {
            }), id, agent));
        }

        @Test
        void changingCategoryRecalculatesSlaFromCreation() {
            Category hardware = category(2L, "Hardware", 12);
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(hardware));
            when(slaConfigRepository.findByCategoryIdAndPriority(2L, Priority.MEDIUM))
                    .thenReturn(Optional.of(slaConfig(hardware, Priority.MEDIUM, 6, 48)));

            ticketServices.updateTicket(update(dto -> dto.setCategoryId(2L)), ticket.getTicketId(), agent);

            assertThat(ticket.getCategory()).isSameAs(hardware);
            assertThat(ticket.getSlaResponseDeadline()).isEqualTo(ticket.getCreatedAt().plusHours(6));
            assertThat(ticket.getSlaResolutionDeadline()).isEqualTo(ticket.getCreatedAt().plusHours(48));
            verify(ticketRepository).save(ticket);
        }

        @Test
        void unknownCategoryReturnsNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketServices.updateTicket(
                    update(dto -> dto.setCategoryId(99L)), ticket.getTicketId(), agent));

            verify(ticketRepository, never()).save(any());
        }

        @Test
        void changingPriorityConfirmsItAndRecalculatesSla() {
            when(slaConfigRepository.findByCategoryIdAndPriority(1L, Priority.CRITICAL))
                    .thenReturn(Optional.of(slaConfig(category, Priority.CRITICAL, 1, 4)));

            ticketServices.updateTicket(update(dto -> dto.setPriority(Priority.CRITICAL)), ticket.getTicketId(), agent);

            assertThat(ticket.getPriority()).isEqualTo(Priority.CRITICAL);
            assertThat(ticket.isPriorityConfirmed()).isTrue();
            assertThat(ticket.getSlaResponseDeadline()).isEqualTo(ticket.getCreatedAt().plusHours(1));
            assertThat(ticket.getSlaResolutionDeadline()).isEqualTo(ticket.getCreatedAt().plusHours(4));
        }

        @Test
        void updatesWithoutCategoryOrPriorityKeepSla() {
            LocalDateTime deadline = ticket.getCreatedAt().plusHours(8);
            ticket.setSlaResponseDeadline(deadline);

            ticketServices.updateTicket(update(dto -> dto.setStatus(TicketStatus.IN_PROGRESS)), ticket.getTicketId(), agent);

            assertThat(ticket.getSlaResponseDeadline()).isEqualTo(deadline);
            verifyNoInteractions(slaConfigRepository);
        }

        @ParameterizedTest
        @EnumSource(value = TicketStatus.class, names = {"RESOLVED", "CLOSED"})
        void finishingTicketSetsResolvedAt(TicketStatus status) {
            LocalDateTime before = LocalDateTime.now();

            ticketServices.updateTicket(update(dto -> dto.setStatus(status)), ticket.getTicketId(), agent);

            assertThat(ticket.getStatus()).isEqualTo(status);
            assertThat(ticket.getResolvedAt()).isBetween(before, LocalDateTime.now());
        }

        @Test
        void closingResolvedTicketKeepsOriginalResolvedAt() {
            LocalDateTime resolvedAt = ticket.getCreatedAt().plusHours(3);
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolvedAt(resolvedAt);

            ticketServices.updateTicket(update(dto -> dto.setStatus(TicketStatus.CLOSED)), ticket.getTicketId(), agent);

            assertThat(ticket.getResolvedAt()).isEqualTo(resolvedAt);
        }

        @Test
        void reopeningTicketClearsResolvedAt() {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolvedAt(ticket.getCreatedAt().plusHours(3));

            ticketServices.updateTicket(update(dto -> dto.setStatus(TicketStatus.IN_PROGRESS)), ticket.getTicketId(), agent);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
            assertThat(ticket.getResolvedAt()).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = Role.RoleName.class, names = {"ROLE_ADMIN", "ROLE_AGENT"})
        void assignsStaffAsAgent(Role.RoleName roleName) {
            User assignee = user(roleName, "assignee@helpdesk.com");
            when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));

            ticketServices.updateTicket(update(dto -> dto.setAgentId(assignee.getId())), ticket.getTicketId(), agent);

            assertThat(ticket.getAgent()).isSameAs(assignee);
            verify(ticketRepository).save(ticket);
        }

        @ParameterizedTest
        @EnumSource(value = Role.RoleName.class, names = {"ROLE_CUSTOMER", "ROLE_VIEWER"})
        void nonStaffCannotBeAssigned(Role.RoleName roleName) {
            User assignee = user(roleName, "assignee@helpdesk.com");
            when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));

            assertThrowsStatus(HttpStatus.BAD_REQUEST, () -> ticketServices.updateTicket(
                    update(dto -> dto.setAgentId(assignee.getId())), ticket.getTicketId(), agent));

            verify(ticketRepository, never()).save(any());
        }

        @Test
        void unknownAgentReturnsNotFound() {
            UUID agentId = UUID.randomUUID();
            when(userRepository.findById(agentId)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketServices.updateTicket(
                    update(dto -> dto.setAgentId(agentId)), ticket.getTicketId(), agent));
        }
    }

    @Nested
    class DeleteTicket {

        @Test
        void deletesTicket() {
            Ticket ticket = ticket(customer, category);
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

            ticketServices.deleteTicket(ticket.getTicketId());

            verify(ticketRepository).delete(ticket);
            verify(ticketRepository).flush();
        }

        @Test
        void unknownTicketReturnsNotFound() {
            UUID id = UUID.randomUUID();
            when(ticketRepository.findById(id)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> ticketServices.deleteTicket(id));

            verify(ticketRepository, never()).delete(any(Ticket.class));
        }

        @Test
        void ticketWithLinkedRecordsReturnsConflict() {
            Ticket ticket = ticket(customer, category);
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));
            doThrow(new DataIntegrityViolationException("fk_ticket_comments")).when(ticketRepository).flush();

            assertThrowsStatus(HttpStatus.CONFLICT, () -> ticketServices.deleteTicket(ticket.getTicketId()));
        }
    }

    static Stream<Arguments> restrictedFields() {
        return Stream.of(
                Arguments.of("categoryId", update(dto -> dto.setCategoryId(2L))),
                Arguments.of("priority", update(dto -> dto.setPriority(Priority.CRITICAL))),
                Arguments.of("status", update(dto -> dto.setStatus(TicketStatus.CLOSED))),
                Arguments.of("agentId", update(dto -> dto.setAgentId(UUID.randomUUID()))));
    }

    private static UpdateTicketDTO update(java.util.function.Consumer<UpdateTicketDTO> changes) {
        UpdateTicketDTO dto = new UpdateTicketDTO();
        changes.accept(dto);
        return dto;
    }

    private static SlaConfig slaConfig(Category category, Priority priority, int responseHours, int resolutionHours) {
        SlaConfig slaConfig = new SlaConfig();
        slaConfig.setId(10L);
        slaConfig.setCategory(category);
        slaConfig.setPriority(priority);
        slaConfig.setResponseTimeInHours(responseHours);
        slaConfig.setResolutionTimeInHours(resolutionHours);
        return slaConfig;
    }
}
