package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.models.AvailableTokens;
import com.bduarte.helpdeskserver.repositories.AvailableTokensRepository;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Builder
public class AvailableTokensService {

    private final AvailableTokensRepository availableTokensRepository;
    private final BCryptPasswordEncoder passwordEncoder;


    public String NewUserToken(UUID userId) {
        List<AvailableTokens> tokens = availableTokensRepository.findByUserId(userId);

        boolean hasValidToken = tokens.stream()
                .anyMatch(tk -> tk.isAvailable() && tk.getExpirationDate().isAfter(LocalDateTime.now()));
        if (hasValidToken) {
            tokens.stream()
                    .filter(AvailableTokens::isAvailable)
                    .forEach(tk -> {
                        tk.setAvailable(false);
                        availableTokensRepository.save(tk);
                    });
        }
        String token = passwordEncoder.encode(UUID.randomUUID().toString());
        AvailableTokens availableTokens = new AvailableTokens(userId, token);

        availableTokensRepository.save(availableTokens);

        return token;
    }
}
