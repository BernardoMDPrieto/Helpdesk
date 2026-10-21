package com.bduarte.helpdeskserver.api.responses;

import com.bduarte.helpdeskserver.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SlaConfigResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private Priority priority;
    private Integer responseTimeInHours;
    private Integer resolutionTimeInHours;
}
