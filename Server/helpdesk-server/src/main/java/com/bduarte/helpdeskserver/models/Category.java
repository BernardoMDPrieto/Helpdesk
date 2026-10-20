package com.bduarte.helpdeskserver.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
public class Category {

    public Category(String categoryName, Integer responseTimeInHoursDefault) {
        this.categoryName = categoryName;
        this.responseTimeInHoursDefault = responseTimeInHoursDefault;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String categoryName;

    @Column(nullable = false)
    private Integer responseTimeInHoursDefault;
}
