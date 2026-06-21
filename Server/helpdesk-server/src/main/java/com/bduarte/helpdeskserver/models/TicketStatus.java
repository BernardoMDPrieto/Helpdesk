package com.bduarte.helpdeskserver.models;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "ticket_statuses")
@Getter
public class TicketStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TicketStatus.Status status;

    public enum Status {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        AWAITING_CUSTOMER,
        CLOSED
    }
}
