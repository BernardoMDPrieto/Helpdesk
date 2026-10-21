package com.bduarte.helpdeskserver.api.filters;

import com.bduarte.helpdeskserver.enums.Priority;
import lombok.Data;

@Data
public class SlaConfigFilter {

    private Long categoryId;

    private Priority priority;
}
