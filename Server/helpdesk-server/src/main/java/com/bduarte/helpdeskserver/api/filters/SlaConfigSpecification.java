package com.bduarte.helpdeskserver.api.filters;

import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.models.SlaConfig;
import org.springframework.data.jpa.domain.Specification;

public class SlaConfigSpecification {
    public static Specification<SlaConfig> byCategoryId(Long categoryId) {
        return (root, query, builder) ->
                builder.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<SlaConfig> byPriority(Priority priority) {
        return (root, query, builder) ->
                builder.equal(root.get("priority"), priority);
    }

    public static Specification<SlaConfig> bySpecification(SlaConfigFilter filter) {

        Specification<SlaConfig> spec =
                (root, query, builder) -> builder.conjunction();

        if (filter == null) {
            return spec;
        }

        if (filter.getCategoryId() != null) {
            spec = spec.and(byCategoryId(filter.getCategoryId()));
        }

        if (filter.getPriority() != null) {
            spec = spec.and(byPriority(filter.getPriority()));
        }

        return spec;
    }
}
