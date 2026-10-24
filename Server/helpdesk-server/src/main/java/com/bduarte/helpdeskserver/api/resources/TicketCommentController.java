package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.requests.CreateTicketCommentDTO;
import com.bduarte.helpdeskserver.api.responses.TicketCommentResponse;
import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.services.TicketCommentServices;
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
@RequestMapping("/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class TicketCommentController {
    private static final Logger logger = LoggerFactory.getLogger(TicketCommentController.class);

    private final TicketCommentServices ticketCommentServices;

    @PostMapping
    public ResponseEntity<TicketCommentResponse> createComment(@PathVariable UUID ticketId,
                                                               @Valid @RequestBody CreateTicketCommentDTO commentDTO,
                                                               @AuthenticationPrincipal UserDetailsImpl currentUser) {
        logger.info("Create comment request received for ticket ID: {} from user: {}", ticketId, currentUser.getUsername());
        TicketCommentResponse comment = ticketCommentServices.createComment(ticketId, commentDTO, currentUser.getUser());
        logger.info("Comment created successfully with ID: {}", comment.getCommentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping
    public ResponseEntity<Page<TicketCommentResponse>> getComments(@PathVariable UUID ticketId,
                                                                   @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
                                                                   @AuthenticationPrincipal UserDetailsImpl currentUser) {
        logger.info("Fetching comments for ticket ID: {} with pagination: page={}, size={}", ticketId, pageable.getPageNumber(), pageable.getPageSize());
        Page<TicketCommentResponse> comments = ticketCommentServices.getComments(ticketId, pageable, currentUser.getUser());
        logger.debug("Comments retrieved successfully, total count: {}", comments.getTotalElements());
        return ResponseEntity.status(HttpStatus.OK).body(comments);
    }
}
