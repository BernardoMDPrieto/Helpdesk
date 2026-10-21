package com.bduarte.helpdeskserver.api.responses;

import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class TicketResponse {
    private UUID ticketId;
    private String title;
    private String description;
    private UUID customerId;
    private String customerName;
    private UUID agentId;
    private String agentName;
    private TicketStatus status;
    private Long categoryId;
    private String categoryName;
    private Priority priority;
    private boolean priorityConfirmed;
    private LocalDateTime slaResponseDeadline;
    private LocalDateTime slaResolutionDeadline;
    private boolean slaResponseBreached;
    private boolean slaResolutionBreached;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
