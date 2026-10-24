package com.bduarte.helpdeskserver.infrastructure.config;

import com.bduarte.helpdeskserver.api.responses.CategoryResponse;
import com.bduarte.helpdeskserver.api.responses.SlaConfigResponse;
import com.bduarte.helpdeskserver.api.responses.TicketCommentResponse;
import com.bduarte.helpdeskserver.api.responses.TicketResponse;
import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.enums.TicketStatus;
import com.bduarte.helpdeskserver.infrastructure.security.JwtAuthFilter;
import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.bduarte.helpdeskserver.models.Role.RoleName.*;
import static com.bduarte.helpdeskserver.support.TestFixtures.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the access rules in {@link SecurityConfig} for every route and role. The real JWT filter
 * runs; only the JWT parsing, the user lookup and the business services are mocked.
 */
@WebMvcTest
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SecurityConfigTest {

    private static final UUID TICKET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Set<Role.RoleName> EVERYONE = EnumSet.allOf(Role.RoleName.class);
    private static final Set<Role.RoleName> ADMIN_ONLY = EnumSet.of(ROLE_ADMIN);
    private static final Set<Role.RoleName> TICKET_USERS = EnumSet.of(ROLE_ADMIN, ROLE_AGENT, ROLE_CUSTOMER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CategoryServices categoryServices;

    @MockitoBean
    private SlaConfigServices slaConfigServices;

    @MockitoBean
    private TicketServices ticketServices;

    @MockitoBean
    private TicketCommentServices ticketCommentServices;

    record Route(HttpMethod method, String path, String body, boolean anonymous, Set<Role.RoleName> roles) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    static List<Route> routes() {
        String ticket = "/tickets/" + TICKET_ID;
        return List.of(
                publicRoute(HttpMethod.POST, "/auth/login", "{\"email\": \"a@helpdesk.com\", \"password\": \"x\"}"),
                publicRoute(HttpMethod.POST, "/auth/register-password", "{\"password\": \"x\", \"token\": \"t\"}"),
                publicRoute(HttpMethod.POST, "/auth/reset-password", "{\"password\": \"x\", \"token\": \"t\"}"),
                publicRoute(HttpMethod.POST, "/auth/request-reset-password", "{\"email\": \"a@helpdesk.com\"}"),

                route(HttpMethod.POST, "/admin/user", "{\"username\": \"Ana\", \"email\": \"ana@helpdesk.com\"}", ADMIN_ONLY),
                route(HttpMethod.GET, "/admin/user", null, ADMIN_ONLY),
                route(HttpMethod.PUT, "/admin/user/" + USER_ID, "{\"active\": true}", ADMIN_ONLY),

                route(HttpMethod.POST, "/config/category", "{\"categoryName\": \"Rede\", \"responseTimeInHoursDefault\": 8}", ADMIN_ONLY),
                route(HttpMethod.GET, "/config/category", null, ADMIN_ONLY),
                route(HttpMethod.GET, "/config/category/1", null, ADMIN_ONLY),
                route(HttpMethod.PUT, "/config/category/1", "{\"categoryName\": \"Redes\"}", ADMIN_ONLY),
                route(HttpMethod.DELETE, "/config/category/1", null, ADMIN_ONLY),

                route(HttpMethod.POST, "/config/sla", "{\"categoryId\": 1, \"priority\": \"HIGH\", \"responseTimeInHours\": 4, \"resolutionTimeInHours\": 24}", ADMIN_ONLY),
                route(HttpMethod.GET, "/config/sla", null, ADMIN_ONLY),
                route(HttpMethod.GET, "/config/sla/1", null, ADMIN_ONLY),
                route(HttpMethod.PUT, "/config/sla/1", "{\"responseTimeInHours\": 2}", ADMIN_ONLY),
                route(HttpMethod.DELETE, "/config/sla/1", null, ADMIN_ONLY),

                route(HttpMethod.POST, "/tickets", "{\"title\": \"T\", \"description\": \"D\", \"categoryId\": 1, \"priority\": \"LOW\"}", EnumSet.of(ROLE_ADMIN, ROLE_CUSTOMER)),
                route(HttpMethod.GET, "/tickets", null, TICKET_USERS),
                route(HttpMethod.GET, ticket, null, TICKET_USERS),
                route(HttpMethod.PUT, ticket, "{\"title\": \"Novo\"}", TICKET_USERS),
                route(HttpMethod.DELETE, ticket, null, ADMIN_ONLY),

                route(HttpMethod.POST, ticket + "/comments", "{\"content\": \"Olá\"}", TICKET_USERS),
                route(HttpMethod.GET, ticket + "/comments", null, TICKET_USERS));
    }

    static Stream<Arguments> accessMatrix() {
        List<Arguments> cases = new ArrayList<>();
        for (Route route : routes()) {
            cases.add(Arguments.of(route, null, route.anonymous()));
            for (Role.RoleName roleName : Role.RoleName.values()) {
                cases.add(Arguments.of(route, roleName, route.roles().contains(roleName)));
            }
        }
        return cases.stream();
    }

    @BeforeEach
    void setUp() {
        // One fake bearer token per role, e.g. "token-ROLE_AGENT"
        Arrays.stream(Role.RoleName.values()).forEach(roleName -> {
            String token = "token-" + roleName.name();
            String email = roleName.name().toLowerCase() + "@helpdesk.com";
            when(jwtService.isValid(token)).thenReturn(true);
            when(jwtService.extractEmail(token)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(new UserDetailsImpl(user(roleName, email)));
        });

        stubServices();
    }

    @ParameterizedTest(name = "{0} as {1} -> allowed={2}")
    @MethodSource("accessMatrix")
    void enforcesAccessRules(Route route, Role.RoleName roleName, boolean allowed) throws Exception {
        MockHttpServletRequestBuilder request = request(route.method(), route.path());
        if (route.body() != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(route.body());
        }
        if (roleName != null) {
            request.header("Authorization", "Bearer token-" + roleName.name());
        }

        mockMvc.perform(request)
                .andExpect(allowed ? status().is2xxSuccessful() : status().isForbidden());
    }

    private void stubServices() {
        UserDetailsImpl principal = new UserDetailsImpl(user(ROLE_CUSTOMER));
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.generateToken(any())).thenReturn("jwt");

        when(userService.getUsers(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        CategoryResponse category = new CategoryResponse(1L, "Rede", 8);
        when(categoryServices.createCategory(any())).thenReturn(category);
        when(categoryServices.getCategories(any(), any())).thenReturn(new PageImpl<>(List.of(category), PageRequest.of(0, 10), 1));
        when(categoryServices.getCategoryById(any())).thenReturn(category);

        SlaConfigResponse sla = new SlaConfigResponse(1L, 1L, "Rede", Priority.HIGH, 4, 24);
        when(slaConfigServices.createSlaConfig(any())).thenReturn(sla);
        when(slaConfigServices.getSlaConfigs(any(), any())).thenReturn(new PageImpl<>(List.of(sla), PageRequest.of(0, 10), 1));
        when(slaConfigServices.getSlaConfigById(any())).thenReturn(sla);

        LocalDateTime now = LocalDateTime.now();
        TicketResponse ticket = new TicketResponse(TICKET_ID, "T", "D", USER_ID, "Ana", null, null, TicketStatus.OPEN,
                1L, "Rede", Priority.LOW, false, now, null, false, false, now, now, null);
        when(ticketServices.createTicket(any(), any())).thenReturn(ticket);
        when(ticketServices.getTickets(any(), any(), any())).thenReturn(new PageImpl<>(List.of(ticket), PageRequest.of(0, 10), 1));
        when(ticketServices.getTicketById(any(), any())).thenReturn(ticket);

        TicketCommentResponse comment = new TicketCommentResponse(UUID.randomUUID(), TICKET_ID, USER_ID, "Ana", "Olá", false, now);
        when(ticketCommentServices.createComment(any(), any(), any())).thenReturn(comment);
        when(ticketCommentServices.getComments(any(), any(), any())).thenReturn(new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1));
    }

    private static Route publicRoute(HttpMethod method, String path, String body) {
        return new Route(method, path, body, true, EVERYONE);
    }

    private static Route route(HttpMethod method, String path, String body, Set<Role.RoleName> roles) {
        return new Route(method, path, body, false, roles);
    }
}
