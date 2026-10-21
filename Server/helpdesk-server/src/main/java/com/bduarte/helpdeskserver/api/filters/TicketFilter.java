package com.bduarte.helpdeskserver.api.filters;

import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class TicketFilter {

    private String title;

    private TicketStatus status;

    private Priority priority;

    private Long categoryId;

    private UUID agentId;

    private UUID customerId;
}
