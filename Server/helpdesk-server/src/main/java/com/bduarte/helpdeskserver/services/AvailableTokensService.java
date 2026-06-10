package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.models.AvailableTokens;
import com.bduarte.helpdeskserver.repositories.AvailableTokensRepository;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Builder
public class AvailableTokensService {

    private final AvailableTokensRepository availableTokensRepository;
    private final BCryptPasswordEncoder passwordEncoder;


    public String newUserToken(UUID userId) {
        List<AvailableTokens> tokens = availableTokensRepository.findByUserId(userId);
        tokens.stream()
                .filter(AvailableTokens::isAvailable)
                .forEach(tk -> {
                    tk.setAvailable(false);
                    availableTokensRepository.save(tk);
                });

        String token = UUID.randomUUID().toString();
        AvailableTokens availableTokens = new AvailableTokens(userId, token);
        availableTokensRepository.save(availableTokens);

        return token;
    }

    public Optional<AvailableTokens> validateToken(String token) {
        return availableTokensRepository
                .findByTokenAndExpirationDateAfterAndAvailableTrue(token, LocalDateTime.now());
    }

    public void saveToken(AvailableTokens availableTokens) {
        availableTokensRepository.save(availableTokens);
    }
}
