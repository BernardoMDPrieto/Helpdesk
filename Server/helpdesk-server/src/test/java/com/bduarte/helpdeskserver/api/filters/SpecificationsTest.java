package com.bduarte.helpdeskserver.api.filters;

import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import com.bduarte.helpdeskserver.models.Category;
import com.bduarte.helpdeskserver.models.SlaConfig;
import com.bduarte.helpdeskserver.models.Ticket;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.support.CriteriaMocks;
import jakarta.persistence.criteria.Expression;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SpecificationsTest {

    @Nested
    class Categories {

        private final CriteriaMocks<Category> criteria = new CriteriaMocks<>();

        @Test
        void nullFilterMatchesEverything() {
            criteria.apply(CategorySpecification.bySpecification(null));

            verify(criteria.builder()).conjunction();
            verifyNoMoreInteractions(criteria.builder());
        }

        @Test
        void blankNameIsIgnored() {
            CategoryFilter filter = new CategoryFilter();
            filter.setCategoryName("  ");

            criteria.apply(CategorySpecification.bySpecification(filter));

            verify(criteria.builder(), never()).like(any(), anyString());
        }

        @Test
        void nameIsCaseInsensitiveContains() {
            Expression<String> lowered = loweredExpression(criteria, "categoryName");
            CategoryFilter filter = new CategoryFilter();
            filter.setCategoryName("ReDe");

            criteria.apply(CategorySpecification.bySpecification(filter));

            verify(criteria.builder()).like(lowered, "%rede%");
        }
    }

    @Nested
    class SlaConfigs {

        private final CriteriaMocks<SlaConfig> criteria = new CriteriaMocks<>();

        @Test
        void nullFilterMatchesEverything() {
            criteria.apply(SlaConfigSpecification.bySpecification(null));

            verify(criteria.builder()).conjunction();
            verifyNoMoreInteractions(criteria.builder());
        }

        @Test
        void filtersByCategoryAndPriority() {
            SlaConfigFilter filter = new SlaConfigFilter();
            filter.setCategoryId(1L);
            filter.setPriority(Priority.HIGH);

            criteria.apply(SlaConfigSpecification.bySpecification(filter));

            verify(criteria.builder()).equal(criteria.path("category.id"), 1L);
            verify(criteria.builder()).equal(criteria.path("priority"), Priority.HIGH);
        }

        @Test
        void emptyFilterAddsNoPredicates() {
            criteria.apply(SlaConfigSpecification.bySpecification(new SlaConfigFilter()));

            verify(criteria.builder(), never()).equal(any(), any(Object.class));
        }
    }

    @Nested
    class Tickets {

        private final CriteriaMocks<Ticket> criteria = new CriteriaMocks<>();

        @Test
        void nullFilterMatchesEverything() {
            criteria.apply(TicketSpecification.bySpecification(null));

            verify(criteria.builder()).conjunction();
            verifyNoMoreInteractions(criteria.builder());
        }

        @Test
        void filtersByEveryField() {
            Expression<String> lowered = loweredExpression(criteria, "title");
            UUID agentId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            TicketFilter filter = new TicketFilter();
            filter.setTitle("Internet");
            filter.setStatus(TicketStatus.OPEN);
            filter.setPriority(Priority.CRITICAL);
            filter.setCategoryId(3L);
            filter.setAgentId(agentId);
            filter.setCustomerId(customerId);

            criteria.apply(TicketSpecification.bySpecification(filter));

            verify(criteria.builder()).like(lowered, "%internet%");
            verify(criteria.builder()).equal(criteria.path("status"), TicketStatus.OPEN);
            verify(criteria.builder()).equal(criteria.path("priority"), Priority.CRITICAL);
            verify(criteria.builder()).equal(criteria.path("category.id"), 3L);
            verify(criteria.builder()).equal(criteria.path("agent.id"), agentId);
            verify(criteria.builder()).equal(criteria.path("customer.id"), customerId);
        }

        @Test
        void blankTitleIsIgnored() {
            TicketFilter filter = new TicketFilter();
            filter.setTitle(" ");

            criteria.apply(TicketSpecification.bySpecification(filter));

            verify(criteria.builder(), never()).like(any(), anyString());
            verify(criteria.builder(), never()).equal(any(), any(Object.class));
        }

        @Test
        void byCustomerIdRestrictsToCustomer() {
            UUID customerId = UUID.randomUUID();

            criteria.apply(TicketSpecification.byCustomerId(customerId));

            verify(criteria.builder()).equal(criteria.path("customer.id"), customerId);
        }
    }

    @Nested
    class Users {

        private final CriteriaMocks<User> criteria = new CriteriaMocks<>();

        @Test
        void nullFilterMatchesEverything() {
            criteria.apply(UserSpecification.bySpecification(null));

            verify(criteria.builder()).conjunction();
            verifyNoMoreInteractions(criteria.builder());
        }

        @Test
        void filtersByNameEmailAndRole() {
            Expression<String> loweredName = loweredExpression(criteria, "userName");
            Expression<String> loweredEmail = loweredExpression(criteria, "email");
            UserFilter filter = new UserFilter();
            filter.setUserName("ANA");
            filter.setEmail("Ana@Helpdesk");
            filter.setRole(2);

            criteria.apply(UserSpecification.bySpecification(filter));

            verify(criteria.builder()).like(loweredName, "%ana%");
            verify(criteria.builder()).like(loweredEmail, "%ana@helpdesk%");
            verify(criteria.builder()).equal(criteria.path("role.id"), 2);
        }

        @Test
        void inactiveStatusFlagDoesNotFilter() {
            UserFilter filter = new UserFilter();
            filter.setStatus(false);

            criteria.apply(UserSpecification.bySpecification(filter));

            verify(criteria.builder(), never()).equal(any(), any(Object.class));
        }

        @Test
        void activeStatusFlagFiltersActiveUsers() {
            UserFilter filter = new UserFilter();
            filter.setStatus(true);

            criteria.apply(UserSpecification.bySpecification(filter));

            verify(criteria.builder()).equal(criteria.path("enabled"), true);
        }
    }

    @SuppressWarnings("unchecked")
    private static Expression<String> loweredExpression(CriteriaMocks<?> criteria, String attribute) {
        // Resolve the path first: creating its mock inside when(...) would nest two stubbings
        Expression<String> path = (Expression<String>) (Expression<?>) criteria.path(attribute);
        Expression<String> lowered = mock(Expression.class, attribute + ".lower()");
        doReturn(lowered).when(criteria.builder()).lower(path);
        return lowered;
    }
}
