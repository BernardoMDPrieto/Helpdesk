package com.bduarte.helpdeskserver.repositories;

import com.bduarte.helpdeskserver.models.AvailableTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvailableTokensRepository extends JpaRepository<AvailableTokens, Long> {

    List<AvailableTokens> findByUserId(UUID id);

    Optional<AvailableTokens> findByTokenAndExpirationDateAfterAndAvailableTrue(String token, LocalDateTime now);
}
