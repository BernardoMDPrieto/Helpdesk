package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.SlaConfigFilter;
import com.bduarte.helpdeskserver.api.filters.SlaConfigSpecification;
import com.bduarte.helpdeskserver.api.requests.CreateSlaConfigDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateSlaConfigDTO;
import com.bduarte.helpdeskserver.api.responses.SlaConfigResponse;
import com.bduarte.helpdeskserver.models.Category;
import com.bduarte.helpdeskserver.models.SlaConfig;
import com.bduarte.helpdeskserver.repositories.CategoryRepository;
import com.bduarte.helpdeskserver.repositories.SlaConfigRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SlaConfigServices {
    private static final Logger logger = LoggerFactory.getLogger(SlaConfigServices.class);
    private final SlaConfigRepository slaConfigRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public SlaConfigResponse createSlaConfig(CreateSlaConfigDTO slaConfigDTO) {
        logger.info("Creating new SLA config for category ID: {} and priority: {}", slaConfigDTO.getCategoryId(), slaConfigDTO.getPriority());

        Category category = categoryRepository.findById(slaConfigDTO.getCategoryId())
                .orElseThrow(() -> {
                    logger.warn("Category not found with ID: {}", slaConfigDTO.getCategoryId());
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Categoria não encontrada"
                    );
                });

        if (slaConfigRepository.existsByCategoryIdAndPriority(slaConfigDTO.getCategoryId(), slaConfigDTO.getPriority())) {
            logger.warn("Attempted to create SLA config that already exists for category ID: {} and priority: {}", slaConfigDTO.getCategoryId(), slaConfigDTO.getPriority());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma configuração de SLA para esta categoria e prioridade"
            );
        }

        SlaConfig slaConfig = new SlaConfig();
        slaConfig.setCategory(category);
        slaConfig.setPriority(slaConfigDTO.getPriority());
        slaConfig.setResponseTimeInHours(slaConfigDTO.getResponseTimeInHours());
        slaConfig.setResolutionTimeInHours(slaConfigDTO.getResolutionTimeInHours());

        SlaConfig savedSlaConfig = slaConfigRepository.save(slaConfig);
        logger.info("SLA config saved successfully with ID: {}", savedSlaConfig.getId());
        return convertSlaConfigResponse(savedSlaConfig);
    }

    public boolean findSLAByCategory(Long categoryId){
        return slaConfigRepository.existsByCategoryId(categoryId);
    }

    public Page<SlaConfigResponse> getSlaConfigs(SlaConfigFilter slaConfigFilter, Pageable pageable) {
        logger.info("Fetching SLA configs with filter: {} and pageable: page={}, size={}", slaConfigFilter, pageable.getPageNumber(), pageable.getPageSize());
        Specification<SlaConfig> spec = SlaConfigSpecification.bySpecification(slaConfigFilter);

        Page<SlaConfigResponse> slaConfigs = slaConfigRepository.findAll(spec, pageable).map(this::convertSlaConfigResponse);
        logger.debug("Retrieved {} SLA configs from database", slaConfigs.getTotalElements());
        return slaConfigs;
    }

    public SlaConfigResponse getSlaConfigById(Long id) {
        logger.info("Fetching SLA config with ID: {}", id);
        return convertSlaConfigResponse(findSlaConfig(id));
    }

    @Transactional
    public void updateSlaConfig(UpdateSlaConfigDTO updateSlaConfigDTO, Long id) {
        logger.info("Updating SLA config with ID: {}", id);
        SlaConfig existingSlaConfig = findSlaConfig(id);

        if (updateSlaConfigDTO.getResponseTimeInHours() != null) {
            existingSlaConfig.setResponseTimeInHours(updateSlaConfigDTO.getResponseTimeInHours());
            logger.debug("Updated response time to: {} for SLA config ID: {}", updateSlaConfigDTO.getResponseTimeInHours(), id);
        }

        if (updateSlaConfigDTO.getResolutionTimeInHours() != null) {
            existingSlaConfig.setResolutionTimeInHours(updateSlaConfigDTO.getResolutionTimeInHours());
            logger.debug("Updated resolution time to: {} for SLA config ID: {}", updateSlaConfigDTO.getResolutionTimeInHours(), id);
        }

        slaConfigRepository.save(existingSlaConfig);
        logger.info("SLA config ID: {} updated successfully", id);
    }

    @Transactional
    public void deleteSlaConfig(Long id) {
        logger.info("Deleting SLA config with ID: {}", id);
        SlaConfig existingSlaConfig = findSlaConfig(id);

        try {
            slaConfigRepository.delete(existingSlaConfig);
            slaConfigRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            logger.warn("SLA config ID: {} is in use and cannot be deleted", id);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Configuração de SLA está em uso e não pode ser removida"
            );
        }
        logger.info("SLA config ID: {} deleted successfully", id);
    }

    private SlaConfig findSlaConfig(Long id) {
        return slaConfigRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("SLA config not found with ID: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Configuração de SLA não encontrada"
                    );
                });
    }

    private SlaConfigResponse convertSlaConfigResponse(SlaConfig slaConfig) {
        return new SlaConfigResponse(
                slaConfig.getId(),
                slaConfig.getCategory().getId(),
                slaConfig.getCategory().getCategoryName(),
                slaConfig.getPriority(),
                slaConfig.getResponseTimeInHours(),
                slaConfig.getResolutionTimeInHours());
    }
}
