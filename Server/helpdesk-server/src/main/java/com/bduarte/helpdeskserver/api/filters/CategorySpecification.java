package com.bduarte.helpdeskserver.api.filters;

import com.bduarte.helpdeskserver.models.Category;
import org.springframework.data.jpa.domain.Specification;

public class CategorySpecification {
    public static Specification<Category> byName(String name) {
        return (root, query, builder) ->
                builder.like(
                        builder.lower(root.get("categoryName")),
                        "%" + name.toLowerCase() + "%"
                );
    }

    public static Specification<Category> bySpecification(CategoryFilter filter) {

        Specification<Category> spec =
                (root, query, builder) -> builder.conjunction();

        if (filter == null) {
            return spec;
        }

        if (filter.getCategoryName() != null && !filter.getCategoryName().isBlank()) {
            spec = spec.and(byName(filter.getCategoryName()));
        }

        return spec;
    }
}
