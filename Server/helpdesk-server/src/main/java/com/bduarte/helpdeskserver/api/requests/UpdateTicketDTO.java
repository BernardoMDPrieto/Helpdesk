package com.bduarte.helpdeskserver.api.requests;

import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTicketDTO {
    private String title;

    private String description;

    private Long categoryId;

    private Priority priority;

    private TicketStatus status;

    private UUID agentId;
}
