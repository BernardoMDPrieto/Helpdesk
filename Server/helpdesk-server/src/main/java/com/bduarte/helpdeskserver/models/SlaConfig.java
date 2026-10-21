package com.bduarte.helpdeskserver.models;

import com.bduarte.helpdeskserver.enums.Priority;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sla_configs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "priority"}))
@Data
@NoArgsConstructor
public class SlaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(nullable = false)
    private Integer responseTimeInHours;

    @Column(nullable = false)
    private Integer resolutionTimeInHours;
}
