package com.bduarte.helpdeskserver.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "AvailableTokens")
@Data
@NoArgsConstructor
public class AvailableTokens {
    public AvailableTokens(UUID userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private boolean available;

    @PrePersist
    public void prePersist() {
        available = true;
        expirationDate = LocalDateTime.now().plusHours(6);
    }
}
