package com.bduarte.helpdeskserver.api.filters;

import com.bduarte.helpdeskserver.models.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    public static Specification<User> byName(String name) {
        return (root, query, builder) ->
                builder.like(
                        builder.lower(root.get("userName")),
                        "%" + name.toLowerCase() + "%"
                );
    }

    public static Specification<User> byEmail(String email) {
        return (root, query, builder) ->
                builder.like(
                        builder.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"
                );
    }

    public static Specification<User> byStatus(Boolean status) {
        return (root, query, builder) ->
                builder.equal(root.get("status"), status);
    }

    public static Specification<User> byRole(Integer role_id) {
        return (root, query, builder) ->
                builder.equal(root.get("role_id"), role_id);
    }

    public static Specification<User> bySpecification(UserFilter filter) {

        if (filter == null) {
            return (root, query, builder) -> builder.conjunction();
        }

        Specification<User> spec =
                (root, query, builder) -> builder.conjunction();

        if (filter.getUserName() != null && !filter.getUserName().isBlank()) {
            spec = spec.and(byName(filter.getUserName()));
        }

        if (filter.getEmail() != null && !filter.getEmail().isBlank()) {
            spec = spec.and(byEmail(filter.getEmail()));
        }

        if (filter.isStatus()) {
            spec = spec.and(byStatus(true));
        }

        if (filter.getRole_id() != null) {
            spec = spec.and(byRole(filter.getRole_id()));
        }

        return spec;
    }


}
