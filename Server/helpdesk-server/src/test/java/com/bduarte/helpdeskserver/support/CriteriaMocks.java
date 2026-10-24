package com.bduarte.helpdeskserver.support;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Runs a {@link Specification} against mocked JPA Criteria objects, so a test can verify which
 * predicates it builds. Every attribute path is a mock named by its dotted path, e.g.
 * {@code path("customer.id")} is what {@code root.get("customer").get("id")} returns.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class CriteriaMocks<T> {

    private final Root<T> root = mock(Root.class, withSettings().strictness(Strictness.LENIENT));
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder builder = mock(CriteriaBuilder.class, withSettings().strictness(Strictness.LENIENT));
    private final Map<String, Path<Object>> paths = new HashMap<>();

    public CriteriaMocks() {
        when(root.get(anyString())).thenAnswer(invocation -> path(invocation.getArgument(0)));
    }

    public void apply(Specification<T> spec) {
        spec.toPredicate(root, query, builder);
    }

    public CriteriaBuilder builder() {
        return builder;
    }

    public Path<Object> path(String dottedPath) {
        return paths.computeIfAbsent(dottedPath, key -> {
            Path<Object> path = mock(Path.class, withSettings().strictness(Strictness.LENIENT).name(key));
            when(path.get(anyString())).thenAnswer(invocation -> path(key + "." + invocation.getArgument(0)));
            return path;
        });
    }
}
