package com.bduarte.helpdeskserver.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_history")
public class TicketHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(nullable = false)
    private String fieldChanged;

    private String oldValue;

    private String newValue;

    private LocalDateTime changedAt;

    @PrePersist
    public void prePersist() {
        changedAt = LocalDateTime.now();
    }

}
