package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.services.JwtService;
import com.bduarte.helpdeskserver.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static com.bduarte.helpdeskserver.support.TestFixtures.standaloneMockMvc;
import static com.bduarte.helpdeskserver.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(authController);
    }

    @Test
    void loginReturnsJwt() throws Exception {
        UserDetailsImpl principal = new UserDetailsImpl(user(Role.RoleName.ROLE_AGENT, "agent@helpdesk.com"));
        when(authManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.generateToken(principal)).thenReturn("jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "agent@helpdesk.com", "password": "Senha@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt-token"));

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("agent@helpdesk.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("Senha@123");
    }

    @Test
    void loginWithBadCredentialsDoesNotIssueToken() {
        when(authManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        // Standalone MockMvc has no security filters to translate the error, so it surfaces as-is
        assertThatThrownBy(() -> mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "agent@helpdesk.com", "password": "wrong"}
                        """)))
                .hasRootCauseInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void registerPasswordUsesToken() throws Exception {
        mockMvc.perform(post("/auth/register-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "NovaSenha@1", "token": "token-123"}
                                """))
                .andExpect(status().isOk());

        verify(userService).registerPassword("NovaSenha@1", "token-123");
    }

    @Test
    void resetPasswordUsesToken() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "NovaSenha@1", "token": "token-123"}
                                """))
                .andExpect(status().isOk());

        verify(userService).registerPassword("NovaSenha@1", "token-123");
    }

    @Test
    void requestResetPasswordSendsEmail() throws Exception {
        mockMvc.perform(post("/auth/request-reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@helpdesk.com"}
                                """))
                .andExpect(status().isOk());

        verify(userService).requestResetPassword("ana@helpdesk.com");
    }

    @Test
    void registerPasswordWithInvalidTokenReturnsBadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado"))
                .when(userService).registerPassword("NovaSenha@1", "expired");

        mockMvc.perform(post("/auth/register-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "NovaSenha@1", "token": "expired"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordWithInvalidTokenReturnsBadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado"))
                .when(userService).registerPassword("NovaSenha@1", "expired");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "NovaSenha@1", "token": "expired"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
