package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.models.AvailableTokens;
import com.bduarte.helpdeskserver.repositories.AvailableTokensRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailableTokensServiceTest {

    @Mock
    private AvailableTokensRepository availableTokensRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private AvailableTokensService availableTokensService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        availableTokensService = AvailableTokensService.builder()
                .availableTokensRepository(availableTokensRepository)
                .passwordEncoder(passwordEncoder)
                .build();
    }

    @Test
    void newUserTokenInvalidatesAvailableTokensAndSavesNewOne() {
        AvailableTokens active = token("old-active", true);
        AvailableTokens used = token("old-used", false);
        when(availableTokensRepository.findByUserId(userId)).thenReturn(List.of(active, used));

        String token = availableTokensService.newUserToken(userId);

        assertThat(active.isAvailable()).isFalse();
        verify(availableTokensRepository).save(active);
        verify(availableTokensRepository, never()).save(used);

        ArgumentCaptor<AvailableTokens> captor = ArgumentCaptor.forClass(AvailableTokens.class);
        verify(availableTokensRepository, times(2)).save(captor.capture());
        AvailableTokens created = captor.getAllValues().get(1);
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getToken()).isEqualTo(token);
        assertThat(UUID.fromString(token)).isNotNull();
    }

    @Test
    void newUserTokenWithoutPreviousTokens() {
        when(availableTokensRepository.findByUserId(userId)).thenReturn(List.of());

        String token = availableTokensService.newUserToken(userId);

        ArgumentCaptor<AvailableTokens> captor = ArgumentCaptor.forClass(AvailableTokens.class);
        verify(availableTokensRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(token);
    }

    @Test
    void eachTokenIsUnique() {
        when(availableTokensRepository.findByUserId(userId)).thenReturn(List.of());

        assertThat(availableTokensService.newUserToken(userId)).isNotEqualTo(availableTokensService.newUserToken(userId));
    }

    @Test
    void validateTokenOnlyAcceptsUnexpiredAvailableTokens() {
        AvailableTokens stored = token("token-123", true);
        when(availableTokensRepository.findByTokenAndExpirationDateAfterAndAvailableTrue(eq("token-123"), any()))
                .thenReturn(Optional.of(stored));

        LocalDateTime before = LocalDateTime.now();
        Optional<AvailableTokens> result = availableTokensService.validateToken("token-123");
        LocalDateTime after = LocalDateTime.now();

        assertThat(result).containsSame(stored);
        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(availableTokensRepository).findByTokenAndExpirationDateAfterAndAvailableTrue(eq("token-123"), now.capture());
        assertThat(now.getValue()).isBetween(before, after);
    }

    @Test
    void validateTokenReturnsEmptyForUnknownToken() {
        when(availableTokensRepository.findByTokenAndExpirationDateAfterAndAvailableTrue(eq("nope"), any()))
                .thenReturn(Optional.empty());

        assertThat(availableTokensService.validateToken("nope")).isEmpty();
    }

    @Test
    void saveTokenDelegatesToRepository() {
        AvailableTokens token = token("token-123", false);

        availableTokensService.saveToken(token);

        verify(availableTokensRepository).save(token);
    }

    private AvailableTokens token(String value, boolean available) {
        AvailableTokens token = new AvailableTokens(userId, value);
        token.prePersist();
        token.setAvailable(available);
        return token;
    }
}
