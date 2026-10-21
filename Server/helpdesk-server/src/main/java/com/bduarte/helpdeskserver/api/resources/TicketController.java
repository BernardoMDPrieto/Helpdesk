package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.TicketFilter;
import com.bduarte.helpdeskserver.api.requests.CreateTicketDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateTicketDTO;
import com.bduarte.helpdeskserver.api.responses.TicketResponse;
import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.services.TicketServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    private final TicketServices ticketServices;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketDTO ticketDTO,
                                                       @AuthenticationPrincipal UserDetailsImpl currentUser) {
        logger.info("Create ticket request received from user: {}", currentUser.getUsername());
        TicketResponse ticket = ticketServices.createTicket(ticketDTO, currentUser.getUser());
        logger.info("Ticket created successfully with ID: {}", ticket.getTicketId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getTickets(@ModelAttribute TicketFilter ticketFilter,
                                                           @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                           @AuthenticationPrincipal UserDetailsImpl currentUser) {
        logger.info("Fetching tickets with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<TicketResponse> tickets = ticketServices.getTickets(ticketFilter, pageable, currentUser.getUser());
        logger.debug("Tickets retrieved successfully, total count: {}", tickets.getTotalElements());
        return ResponseEntity.status(HttpStatus.OK).body(tickets);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID ticketId,
                                                    @AuthenticationPrincipal UserDetailsImpl currentUser) {
        logger.info("Fetching ticket with ID: {}", ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(ticketServices.getTicketById(ticketId, currentUser.getUser()));
    }

    @PutMapping("/{ticketId}")
    public ResponseEntity<Void> updateTicket(@Valid @RequestBody UpdateTicketDTO ticketDTO, @PathVariable UUID ticketId,
                                             @AuthenticationPrincipal UserDetailsImpl currentUser) {
        logger.info("Update ticket request received for ticket ID: {}", ticketId);
        ticketServices.updateTicket(ticketDTO, ticketId, currentUser.getUser());
        logger.info("Ticket updated successfully with ID: {}", ticketId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID ticketId) {
        logger.info("Delete ticket request received for ticket ID: {}", ticketId);
        ticketServices.deleteTicket(ticketId);
        logger.info("Ticket deleted successfully with ID: {}", ticketId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
