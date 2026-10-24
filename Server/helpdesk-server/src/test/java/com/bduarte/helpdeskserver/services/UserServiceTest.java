package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.UserFilter;
import com.bduarte.helpdeskserver.api.requests.CreateUserDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateUserDTO;
import com.bduarte.helpdeskserver.api.responses.UserResponse;
import com.bduarte.helpdeskserver.models.AvailableTokens;
import com.bduarte.helpdeskserver.models.ResetPasswordEvent;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.models.UserRegisteredEvent;
import com.bduarte.helpdeskserver.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.bduarte.helpdeskserver.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AvailableTokensService availableTokensService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = UserService.builder()
                .publisher(publisher)
                .userRepository(userRepository)
                .emailService(emailService)
                .availableTokensService(availableTokensService)
                .passwordEncoder(passwordEncoder)
                .build();
    }

    @Nested
    class CreateNewUser {

        @Test
        void savesUserWithRandomPasswordAndPublishesRegistration() throws Exception {
            Role agentRole = role(Role.RoleName.ROLE_AGENT);
            UUID savedId = UUID.randomUUID();
            when(userRepository.findByEmail("ana@helpdesk.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-random");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(savedId);
                return user;
            });
            when(availableTokensService.newUserToken(savedId)).thenReturn("token-123");

            userService.CreateNewUser(new CreateUserDTO("Ana", "ana@helpdesk.com", agentRole));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getEmail()).isEqualTo("ana@helpdesk.com");
            assertThat(saved.getUserName()).isEqualTo("Ana");
            assertThat(saved.getPassword()).isEqualTo("encoded-random");
            assertThat(saved.getRole()).isSameAs(agentRole);

            // The temporary password is random, so it must not be guessable from the user's data
            ArgumentCaptor<String> rawPassword = ArgumentCaptor.forClass(String.class);
            verify(passwordEncoder).encode(rawPassword.capture());
            assertThat(UUID.fromString(rawPassword.getValue())).isNotNull();

            verify(publisher).publishEvent(new UserRegisteredEvent("ana@helpdesk.com", "Ana", "token-123"));
        }

        @Test
        void duplicateEmailReturnsConflict() {
            when(userRepository.findByEmail("ana@helpdesk.com")).thenReturn(Optional.of(user(Role.RoleName.ROLE_AGENT, "ana@helpdesk.com")));

            assertThrowsStatus(HttpStatus.CONFLICT, () -> userService.CreateNewUser(
                    new CreateUserDTO("Ana", "ana@helpdesk.com", role(Role.RoleName.ROLE_AGENT))));

            verify(userRepository, never()).save(any());
            verifyNoInteractions(availableTokensService, publisher);
        }
    }

    @Nested
    class RequestResetPassword {

        @Test
        void generatesTokenAndPublishesResetEvent() {
            User user = user(Role.RoleName.ROLE_CUSTOMER);
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(availableTokensService.newUserToken(user.getId())).thenReturn("reset-token");

            userService.requestResetPassword(user.getEmail());

            verify(publisher).publishEvent(new ResetPasswordEvent(user.getEmail(), user.getUserName(), "reset-token"));
        }

        @Test
        void unknownEmailIsIgnoredWithoutRevealingIt() {
            when(userRepository.findByEmail("ghost@helpdesk.com")).thenReturn(Optional.empty());

            assertThatCode(() -> userService.requestResetPassword("ghost@helpdesk.com")).doesNotThrowAnyException();

            verifyNoInteractions(availableTokensService, publisher);
        }
    }

    @Nested
    class RegisterPassword {

        @Test
        void setsEncodedPasswordAndInvalidatesToken() {
            User user = user(Role.RoleName.ROLE_CUSTOMER);
            AvailableTokens token = availableToken(user.getId(), "token-123");
            when(availableTokensService.validateToken("token-123")).thenReturn(Optional.of(token));
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NovaSenha@1")).thenReturn("encoded-new");

            userService.registerPassword("NovaSenha@1", "token-123");

            assertThat(user.getPassword()).isEqualTo("encoded-new");
            verify(userRepository).save(user);
            assertThat(token.isAvailable()).isFalse();
            verify(availableTokensService).saveToken(token);
        }

        @Test
        void invalidTokenReturnsBadRequest() {
            when(availableTokensService.validateToken("expired")).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.BAD_REQUEST, () -> userService.registerPassword("NovaSenha@1", "expired"));

            verifyNoInteractions(userRepository, passwordEncoder);
            verify(availableTokensService, never()).saveToken(any());
        }

        @Test
        void tokenForMissingUserReturnsNotFound() {
            UUID userId = UUID.randomUUID();
            AvailableTokens token = availableToken(userId, "token-123");
            when(availableTokensService.validateToken("token-123")).thenReturn(Optional.of(token));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> userService.registerPassword("NovaSenha@1", "token-123"));

            verify(userRepository, never()).save(any());
            assertThat(token.isAvailable()).isTrue();
            verify(availableTokensService, never()).saveToken(any());
        }
    }

    @Test
    void getUsersMapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = user(Role.RoleName.ROLE_AGENT);
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        Page<UserResponse> page = userService.getUsers(new UserFilter(), pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getPageable()).isEqualTo(pageable);
        assertThat(page.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(user.getId());
            assertThat(response.getEmail()).isEqualTo(user.getEmail());
            assertThat(response.getUserName()).isEqualTo(user.getUserName());
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getRole()).isSameAs(user.getRole());
        });
    }

    @Nested
    class UpdateUser {

        @Test
        void updatesNameStatusAndRole() {
            User user = user(Role.RoleName.ROLE_CUSTOMER);
            Role agentRole = role(Role.RoleName.ROLE_AGENT);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            userService.updateUser(new UpdateUserDTO("Novo Nome", false, agentRole), user.getId());

            assertThat(user.getUserName()).isEqualTo("Novo Nome");
            assertThat(user.getEnabled()).isFalse();
            assertThat(user.getRole()).isSameAs(agentRole);
            verify(userRepository).save(user);
        }

        @Test
        void nullNameAndRoleAreKept() {
            User user = user(Role.RoleName.ROLE_CUSTOMER);
            Role originalRole = user.getRole();
            String originalName = user.getUserName();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            userService.updateUser(new UpdateUserDTO(null, true, null), user.getId());

            assertThat(user.getUserName()).isEqualTo(originalName);
            assertThat(user.getRole()).isSameAs(originalRole);
            assertThat(user.getEnabled()).isTrue();
        }

        @Test
        void missingActiveFlagKeepsCurrentStatus() {
            User user = user(Role.RoleName.ROLE_CUSTOMER);
            user.setEnabled(false);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            userService.updateUser(new UpdateUserDTO("Novo Nome", null, null), user.getId());

            assertThat(user.getEnabled()).isFalse();
            assertThat(user.getUserName()).isEqualTo("Novo Nome");
            verify(userRepository).save(user);
        }

        @Test
        void unknownUserReturnsNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThrowsStatus(HttpStatus.NOT_FOUND, () -> userService.updateUser(new UpdateUserDTO("Nome", true, null), id));

            verify(userRepository, never()).save(any());
        }
    }

    private static AvailableTokens availableToken(UUID userId, String value) {
        AvailableTokens token = new AvailableTokens(userId, value);
        token.prePersist();
        return token;
    }
}
