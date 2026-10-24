package com.bduarte.helpdeskserver.infrastructure.security;

import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.services.JwtService;
import com.bduarte.helpdeskserver.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.bduarte.helpdeskserver.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest("GET", "/tickets");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesUser() throws Exception {
        UserDetailsImpl details = new UserDetailsImpl(user(Role.RoleName.ROLE_AGENT, "agent@helpdesk.com"));
        request.addHeader("Authorization", "Bearer good-token");
        when(jwtService.isValid("good-token")).thenReturn(true);
        when(jwtService.extractEmail("good-token")).thenReturn("agent@helpdesk.com");
        when(userDetailsService.loadUserByUsername("agent@helpdesk.com")).thenReturn(details);

        jwtAuthFilter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isSameAs(details);
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_AGENT");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void invalidTokenLeavesRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtService.isValid("bad-token")).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractEmail(anyString());
        verifyNoInteractions(userDetailsService);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void missingHeaderLeavesRequestAnonymous() throws Exception {
        jwtAuthFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void nonBearerHeaderIsIgnored() throws Exception {
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");

        jwtAuthFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
