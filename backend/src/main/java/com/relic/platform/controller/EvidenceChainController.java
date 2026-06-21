package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.EvidenceLog;
import com.relic.platform.service.EvidenceChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceChainController {

    @Autowired
    private EvidenceChainService evidenceChainService;

    @GetMapping("/")
    public ApiResponse<PageResult<EvidenceLog>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long artifactId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword) {
        try {
            Page<EvidenceLog> evidencePage = evidenceChainService.list(page, size, artifactId, action, keyword);
            return ApiResponse.success(PageResult.from(evidencePage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/artifact/{artifactId}")
    public ApiResponse<List<EvidenceLog>> listByArtifact(@PathVariable Long artifactId) {
        try {
            List<EvidenceLog> logs = evidenceChainService.listByArtifact(artifactId);
            return ApiResponse.success(logs);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
