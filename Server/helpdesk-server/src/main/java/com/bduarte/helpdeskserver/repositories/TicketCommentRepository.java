package com.bduarte.helpdeskserver.repositories;

import com.bduarte.helpdeskserver.models.TicketComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {
    @EntityGraph(attributePaths = "user")
    Page<TicketComment> findByTicketTicketId(UUID ticketId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<TicketComment> findByTicketTicketIdAndIsInternalFalse(UUID ticketId, Pageable pageable);
}
