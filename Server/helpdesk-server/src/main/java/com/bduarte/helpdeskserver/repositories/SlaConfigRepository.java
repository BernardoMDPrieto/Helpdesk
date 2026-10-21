package com.bduarte.helpdeskserver.repositories;

import com.bduarte.helpdeskserver.enums.Priority;
import com.bduarte.helpdeskserver.models.SlaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlaConfigRepository extends JpaRepository<SlaConfig, Long>, JpaSpecificationExecutor<SlaConfig> {
    boolean existsByCategoryIdAndPriority(Long categoryId, Priority priority);

    boolean existsByCategoryId(Long categoryId);

    Optional<SlaConfig> findByCategoryIdAndPriority(Long categoryId, Priority priority);
}
