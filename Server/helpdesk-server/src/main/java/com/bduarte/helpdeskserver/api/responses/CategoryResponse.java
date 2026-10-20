package com.bduarte.helpdeskserver.api.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CategoryResponse {
    private Long id;
    private String categoryName;
    private Integer responseTimeInHoursDefault;
}
