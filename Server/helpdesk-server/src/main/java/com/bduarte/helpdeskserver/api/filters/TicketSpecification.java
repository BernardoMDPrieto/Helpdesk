package com.bduarte.helpdeskserver.api.filters;

import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import com.bduarte.helpdeskserver.models.Ticket;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class TicketSpecification {
    public static Specification<Ticket> byTitle(String title) {
        return (root, query, builder) ->
                builder.like(
                        builder.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"
                );
    }

    public static Specification<Ticket> byStatus(TicketStatus status) {
        return (root, query, builder) ->
                builder.equal(root.get("status"), status);
    }

    public static Specification<Ticket> byPriority(Priority priority) {
        return (root, query, builder) ->
                builder.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> byCategoryId(Long categoryId) {
        return (root, query, builder) ->
                builder.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Ticket> byAgentId(UUID agentId) {
        return (root, query, builder) ->
                builder.equal(root.get("agent").get("id"), agentId);
    }

    public static Specification<Ticket> byCustomerId(UUID customerId) {
        return (root, query, builder) ->
                builder.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Ticket> bySpecification(TicketFilter filter) {

        Specification<Ticket> spec =
                (root, query, builder) -> builder.conjunction();

        if (filter == null) {
            return spec;
        }

        if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
            spec = spec.and(byTitle(filter.getTitle()));
        }

        if (filter.getStatus() != null) {
            spec = spec.and(byStatus(filter.getStatus()));
        }

        if (filter.getPriority() != null) {
            spec = spec.and(byPriority(filter.getPriority()));
        }

        if (filter.getCategoryId() != null) {
            spec = spec.and(byCategoryId(filter.getCategoryId()));
        }

        if (filter.getAgentId() != null) {
            spec = spec.and(byAgentId(filter.getAgentId()));
        }

        if (filter.getCustomerId() != null) {
            spec = spec.and(byCustomerId(filter.getCustomerId()));
        }

        return spec;
    }
}
