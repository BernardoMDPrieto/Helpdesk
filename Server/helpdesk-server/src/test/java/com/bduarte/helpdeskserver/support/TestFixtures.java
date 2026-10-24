package com.bduarte.helpdeskserver.support;

import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.models.Category;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.Ticket;
import com.bduarte.helpdeskserver.models.User;
import org.assertj.core.api.ThrowableAssert;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Role role(Role.RoleName roleName) {
        // Role has no setters because roles are seeded in the database
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", (long) roleName.ordinal() + 1);
        ReflectionTestUtils.setField(role, "name", roleName);
        return role;
    }

    public static User user(Role.RoleName roleName) {
        return user(roleName, roleName.name().replace("ROLE_", "").toLowerCase() + "@helpdesk.com");
    }

    public static User user(Role.RoleName roleName, String email) {
        User user = new User(email, "User " + roleName.name(), "encoded-password", role(roleName));
        user.setId(UUID.randomUUID());
        return user;
    }

    public static Category category(Long id, String name, int responseTimeInHoursDefault) {
        Category category = new Category(name, responseTimeInHoursDefault);
        category.setId(id);
        return category;
    }

    public static Ticket ticket(User customer, Category category) {
        Ticket ticket = new Ticket();
        ticket.setTicketId(UUID.randomUUID());
        ticket.setCustomer(customer);
        ticket.setCategory(category);
        ticket.setTitle("Sem internet");
        ticket.setDescription("A internet caiu desde cedo");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(Priority.MEDIUM);
        ticket.setCreatedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        ticket.setUpdatedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        return ticket;
    }

    /**
     * MockMvc around a single controller, without a Spring context. Resolves {@code Pageable}
     * and {@code @AuthenticationPrincipal} the same way the application does.
     */
    public static MockMvc standaloneMockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    public static void authenticate(User user) {
        UserDetailsImpl principal = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    public static void assertThrowsStatus(HttpStatus status, ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(status));
    }
}
