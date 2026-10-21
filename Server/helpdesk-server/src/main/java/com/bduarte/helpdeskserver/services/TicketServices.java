package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.TicketFilter;
import com.bduarte.helpdeskserver.api.filters.TicketSpecification;
import com.bduarte.helpdeskserver.api.requests.CreateTicketDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateTicketDTO;
import com.bduarte.helpdeskserver.api.responses.TicketResponse;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import com.bduarte.helpdeskserver.models.*;
import com.bduarte.helpdeskserver.repositories.*;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketServices {
    private static final Logger logger = LoggerFactory.getLogger(TicketServices.class);
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final SlaConfigRepository slaConfigRepository;
    private final UserRepository userRepository;

    @Transactional
    public TicketResponse createTicket(CreateTicketDTO ticketDTO, User currentUser) {
        logger.info("Creating new ticket for user ID: {} in category ID: {}", currentUser.getId(), ticketDTO.getCategoryId());

        Ticket ticket = new Ticket();
        ticket.setTitle(ticketDTO.getTitle());
        ticket.setDescription(ticketDTO.getDescription());
        ticket.setCustomer(currentUser);
        ticket.setCategory(findCategory(ticketDTO.getCategoryId()));
        ticket.setPriority(ticketDTO.getPriority());
        // Priority set by a customer must be confirmed later by an admin or agent
        ticket.setPriorityConfirmed(!isCustomer(currentUser));
        ticket.setStatus(TicketStatus.OPEN);
        applySlaDeadlines(ticket, LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);
        logger.info("Ticket saved successfully with ID: {}", savedTicket.getTicketId());
        return convertTicketResponse(savedTicket);
    }

    public Page<TicketResponse> getTickets(TicketFilter ticketFilter, Pageable pageable, User currentUser) {
        logger.info("Fetching tickets with filter: {} and pageable: page={}, size={}", ticketFilter, pageable.getPageNumber(), pageable.getPageSize());
        Specification<Ticket> spec = TicketSpecification.bySpecification(ticketFilter);

        if (isCustomer(currentUser)) {
            spec = spec.and(TicketSpecification.byCustomerId(currentUser.getId()));
        }

        Page<TicketResponse> tickets = ticketRepository.findAll(spec, pageable).map(this::convertTicketResponse);
        logger.debug("Retrieved {} tickets from database", tickets.getTotalElements());
        return tickets;
    }

    public TicketResponse getTicketById(UUID id, User currentUser) {
        logger.info("Fetching ticket with ID: {}", id);
        return convertTicketResponse(findAccessibleTicket(id, currentUser));
    }

    @Transactional
    public void updateTicket(UpdateTicketDTO updateTicketDTO, UUID id, User currentUser) {
        logger.info("Updating ticket with ID: {}", id);
        Ticket existingTicket = findAccessibleTicket(id, currentUser);

        if (isCustomer(currentUser) && (updateTicketDTO.getCategoryId() != null
                || updateTicketDTO.getPriority() != null
                || updateTicketDTO.getStatus() != null
                || updateTicketDTO.getAgentId() != null)) {
            logger.warn("Customer ID: {} attempted to change restricted fields on ticket ID: {}", currentUser.getId(), id);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cliente só pode alterar o título e a descrição do ticket"
            );
        }

        if (updateTicketDTO.getTitle() != null && !updateTicketDTO.getTitle().isBlank()) {
            existingTicket.setTitle(updateTicketDTO.getTitle());
            logger.debug("Updated title for ticket ID: {}", id);
        }

        if (updateTicketDTO.getDescription() != null && !updateTicketDTO.getDescription().isBlank()) {
            existingTicket.setDescription(updateTicketDTO.getDescription());
            logger.debug("Updated description for ticket ID: {}", id);
        }

        boolean slaChanged = false;

        if (updateTicketDTO.getCategoryId() != null) {
            existingTicket.setCategory(findCategory(updateTicketDTO.getCategoryId()));
            slaChanged = true;
            logger.debug("Updated category to: {} for ticket ID: {}", updateTicketDTO.getCategoryId(), id);
        }

        if (updateTicketDTO.getPriority() != null) {
            existingTicket.setPriority(updateTicketDTO.getPriority());
            existingTicket.setPriorityConfirmed(true);
            slaChanged = true;
            logger.debug("Updated priority to: {} for ticket ID: {}", updateTicketDTO.getPriority(), id);
        }

        if (slaChanged) {
            applySlaDeadlines(existingTicket, existingTicket.getCreatedAt());
        }

        if (updateTicketDTO.getStatus() != null) {
            existingTicket.setStatus(updateTicketDTO.getStatus());
            if (isFinished(updateTicketDTO.getStatus())) {
                if (existingTicket.getResolvedAt() == null) {
                    existingTicket.setResolvedAt(LocalDateTime.now());
                }
            } else {
                existingTicket.setResolvedAt(null);
            }
            logger.debug("Updated status to: {} for ticket ID: {}", updateTicketDTO.getStatus(), id);
        }

        if (updateTicketDTO.getAgentId() != null) {
            existingTicket.setAgent(findAgent(updateTicketDTO.getAgentId()));
            logger.debug("Assigned agent ID: {} to ticket ID: {}", updateTicketDTO.getAgentId(), id);
        }

        ticketRepository.save(existingTicket);
        logger.info("Ticket ID: {} updated successfully", id);
    }

    @Transactional
    public void deleteTicket(UUID id) {
        logger.info("Deleting ticket with ID: {}", id);
        Ticket existingTicket = findTicket(id);

        try {
            ticketRepository.delete(existingTicket);
            ticketRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            logger.warn("Ticket ID: {} has related records and cannot be deleted", id);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ticket possui registros vinculados e não pode ser removido"
            );
        }
        logger.info("Ticket ID: {} deleted successfully", id);
    }

    private void applySlaDeadlines(Ticket ticket, LocalDateTime baseTime) {
        Category category = ticket.getCategory();
        slaConfigRepository.findByCategoryIdAndPriority(category.getId(), ticket.getPriority())
                .ifPresentOrElse(slaConfig -> {
                    ticket.setSlaResponseDeadline(baseTime.plusHours(slaConfig.getResponseTimeInHours()));
                    ticket.setSlaResolutionDeadline(baseTime.plusHours(slaConfig.getResolutionTimeInHours()));
                }, () -> {
                    logger.debug("No SLA config for category ID: {} and priority: {}, using category default", category.getId(), ticket.getPriority());
                    ticket.setSlaResponseDeadline(baseTime.plusHours(category.getResponseTimeInHoursDefault()));
                    ticket.setSlaResolutionDeadline(null);
                });
    }

    private Ticket findAccessibleTicket(UUID id, User currentUser) {
        Ticket ticket = findTicket(id);
        // Customers must not learn about tickets from other customers, so they get the same 404
        if (isCustomer(currentUser) && !ticket.getCustomer().getId().equals(currentUser.getId())) {
            logger.warn("Customer ID: {} attempted to access ticket ID: {} from another customer", currentUser.getId(), id);
            throw ticketNotFound();
        }
        return ticket;
    }

    private Ticket findTicket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Ticket not found with ID: {}", id);
                    return ticketNotFound();
                });
    }

    private ResponseStatusException ticketNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ticket não encontrado"
        );
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

    private User findAgent(UUID id) {
        User agent = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Agent not found with ID: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Agente não encontrado"
                    );
                });

        Role.RoleName roleName = agent.getRole().getName();
        if (roleName != Role.RoleName.ROLE_AGENT && roleName != Role.RoleName.ROLE_ADMIN) {
            logger.warn("User ID: {} with role {} cannot be assigned as agent", id, roleName);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário informado não é um agente"
            );
        }
        return agent;
    }

    private boolean isCustomer(User user) {
        return user.getRole().getName() == Role.RoleName.ROLE_CUSTOMER;
    }

    private boolean isFinished(TicketStatus status) {
        return status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED;
    }

    private TicketResponse convertTicketResponse(Ticket ticket) {
        User agent = ticket.getAgent();
        return new TicketResponse(
                ticket.getTicketId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCustomer().getId(),
                ticket.getCustomer().getUserName(),
                agent != null ? agent.getId() : null,
                agent != null ? agent.getUserName() : null,
                ticket.getStatus(),
                ticket.getCategory().getId(),
                ticket.getCategory().getCategoryName(),
                ticket.getPriority(),
                ticket.isPriorityConfirmed(),
                ticket.getSlaResponseDeadline(),
                ticket.getSlaResolutionDeadline(),
                ticket.isSlaResponseBreached(),
                ticket.isSlaResolutionBreached(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt());
    }
}
