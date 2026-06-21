package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.models.AvailableTokens;
import com.bduarte.helpdeskserver.repositories.AvailableTokensRepository;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Builder
public class AvailableTokensService {
    private static final Logger logger = LoggerFactory.getLogger(AvailableTokensService.class);

    private final AvailableTokensRepository availableTokensRepository;
    private final BCryptPasswordEncoder passwordEncoder;


    public String newUserToken(UUID userId) {
        logger.info("Generating new token for user ID: {}", userId);
        List<AvailableTokens> tokens = availableTokensRepository.findByUserId(userId);
        logger.debug("Found {} existing tokens for user ID: {}", tokens.size(), userId);
        
        tokens.stream()
                .filter(AvailableTokens::isAvailable)
                .forEach(tk -> {
                    tk.setAvailable(false);
                    availableTokensRepository.save(tk);
                    logger.debug("Invalidated existing token for user ID: {}", userId);
                });

        String token = UUID.randomUUID().toString();
        AvailableTokens availableTokens = new AvailableTokens(userId, token);
        availableTokensRepository.save(availableTokens);
        logger.info("New token generated and saved for user ID: {}", userId);

        return token;
    }

    public Optional<AvailableTokens> validateToken(String token) {
        logger.debug("Validating token");
        Optional<AvailableTokens> result = availableTokensRepository
                .findByTokenAndExpirationDateAfterAndAvailableTrue(token, LocalDateTime.now());
        
        if (result.isPresent()) {
            logger.debug("Token is valid and available");
        } else {
            logger.warn("Token validation failed - token is either invalid, expired, or not available");
        }
        return result;
    }

    public void saveToken(AvailableTokens availableTokens) {
        logger.debug("Saving token for user ID: {}", availableTokens.getUserId());
        availableTokensRepository.save(availableTokens);
        logger.debug("Token saved successfully for user ID: {}", availableTokens.getUserId());
    }
}
