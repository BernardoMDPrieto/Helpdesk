package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.requests.CreateTicketCommentDTO;
import com.bduarte.helpdeskserver.api.responses.TicketCommentResponse;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.Ticket;
import com.bduarte.helpdeskserver.models.TicketComment;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.repositories.TicketCommentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketCommentServices {
    private static final Logger logger = LoggerFactory.getLogger(TicketCommentServices.class);
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketServices ticketServices;

    @Transactional
    public TicketCommentResponse createComment(UUID ticketId, CreateTicketCommentDTO commentDTO, User currentUser) {
        logger.info("Creating comment on ticket ID: {} by user ID: {}", ticketId, currentUser.getId());
        Ticket ticket = ticketServices.findAccessibleTicket(ticketId, currentUser);

        boolean internal = Boolean.TRUE.equals(commentDTO.getInternal());
        if (internal && !isStaff(currentUser)) {
            logger.warn("User ID: {} attempted to create an internal comment on ticket ID: {}", currentUser.getId(), ticketId);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Apenas administradores e agentes podem criar comentários internos"
            );
        }

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setUser(currentUser);
        comment.setContent(commentDTO.getContent());
        comment.setInternal(internal);

        TicketComment savedComment = ticketCommentRepository.save(comment);
        logger.info("Comment saved successfully with ID: {} on ticket ID: {}", savedComment.getId(), ticketId);
        return convertTicketCommentResponse(savedComment);
    }

    public Page<TicketCommentResponse> getComments(UUID ticketId, Pageable pageable, User currentUser) {
        logger.info("Fetching comments for ticket ID: {} with pageable: page={}, size={}", ticketId, pageable.getPageNumber(), pageable.getPageSize());
        ticketServices.findAccessibleTicket(ticketId, currentUser);

        // Internal comments are staff notes and must never reach customers
        Page<TicketComment> comments = isStaff(currentUser)
                ? ticketCommentRepository.findByTicketTicketId(ticketId, pageable)
                : ticketCommentRepository.findByTicketTicketIdAndIsInternalFalse(ticketId, pageable);

        logger.debug("Retrieved {} comments for ticket ID: {}", comments.getTotalElements(), ticketId);
        return comments.map(this::convertTicketCommentResponse);
    }

    private boolean isStaff(User user) {
        Role.RoleName roleName = user.getRole().getName();
        return roleName == Role.RoleName.ROLE_ADMIN || roleName == Role.RoleName.ROLE_AGENT;
    }

    private TicketCommentResponse convertTicketCommentResponse(TicketComment comment) {
        return new TicketCommentResponse(
                comment.getId(),
                comment.getTicket().getTicketId(),
                comment.getUser().getId(),
                comment.getUser().getUserName(),
                comment.getContent(),
                comment.isInternal(),
                comment.getCreatedAt());
    }
}
