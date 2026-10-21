package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.SlaConfigFilter;
import com.bduarte.helpdeskserver.api.requests.CreateSlaConfigDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateSlaConfigDTO;
import com.bduarte.helpdeskserver.api.responses.SlaConfigResponse;
import com.bduarte.helpdeskserver.services.SlaConfigServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class SlaConfigController {
    private static final Logger logger = LoggerFactory.getLogger(SlaConfigController.class);

    private final SlaConfigServices slaConfigServices;

    @PostMapping("/sla")
    public ResponseEntity<SlaConfigResponse> createSlaConfig(@Valid @RequestBody CreateSlaConfigDTO slaConfigDTO) {
        logger.info("Create SLA config request received for category ID: {} and priority: {}", slaConfigDTO.getCategoryId(), slaConfigDTO.getPriority());
        SlaConfigResponse slaConfig = slaConfigServices.createSlaConfig(slaConfigDTO);
        logger.info("SLA config created successfully with ID: {}", slaConfig.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(slaConfig);
    }

    @GetMapping("/sla")
    public ResponseEntity<Page<SlaConfigResponse>> getSlaConfigs(@ModelAttribute SlaConfigFilter slaConfigFilter,
                                                                 @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        logger.info("Fetching SLA configs with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<SlaConfigResponse> slaConfigs = slaConfigServices.getSlaConfigs(slaConfigFilter, pageable);
        logger.debug("SLA configs retrieved successfully, total count: {}", slaConfigs.getTotalElements());
        return ResponseEntity.status(HttpStatus.OK).body(slaConfigs);
    }

    @GetMapping("/sla/{slaConfigId}")
    public ResponseEntity<SlaConfigResponse> getSlaConfig(@PathVariable Long slaConfigId) {
        logger.info("Fetching SLA config with ID: {}", slaConfigId);
        return ResponseEntity.status(HttpStatus.OK).body(slaConfigServices.getSlaConfigById(slaConfigId));
    }

    @PutMapping("/sla/{slaConfigId}")
    public ResponseEntity<Void> updateSlaConfig(@Valid @RequestBody UpdateSlaConfigDTO slaConfigDTO, @PathVariable Long slaConfigId) {
        logger.info("Update SLA config request received for SLA config ID: {}", slaConfigId);
        slaConfigServices.updateSlaConfig(slaConfigDTO, slaConfigId);
        logger.info("SLA config updated successfully with ID: {}", slaConfigId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/sla/{slaConfigId}")
    public ResponseEntity<Void> deleteSlaConfig(@PathVariable Long slaConfigId) {
        logger.info("Delete SLA config request received for SLA config ID: {}", slaConfigId);
        slaConfigServices.deleteSlaConfig(slaConfigId);
        logger.info("SLA config deleted successfully with ID: {}", slaConfigId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
