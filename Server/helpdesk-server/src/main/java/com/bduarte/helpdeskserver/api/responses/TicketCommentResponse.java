package com.bduarte.helpdeskserver.api.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class TicketCommentResponse {
    private UUID commentId;
    private UUID ticketId;
    private UUID authorId;
    private String authorName;
    private String content;
    private boolean internal;
    private LocalDateTime createdAt;
}
