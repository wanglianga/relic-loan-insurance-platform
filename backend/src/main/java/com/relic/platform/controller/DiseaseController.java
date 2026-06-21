package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.Disease;
import com.relic.platform.service.DiseaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diseases")
public class DiseaseController {

    @Autowired
    private DiseaseService diseaseService;

    @GetMapping("/")
    public ApiResponse<PageResult<Disease>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Disease> diseasePage = diseaseService.list(page, size);
            return ApiResponse.success(PageResult.from(diseasePage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/artifact/{artifactId}")
    public ApiResponse<List<Disease>> listByArtifact(@PathVariable Long artifactId) {
        try {
            List<Disease> diseases = diseaseService.listByArtifact(artifactId);
            return ApiResponse.success(diseases);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/")
    public ApiResponse<Disease> create(@RequestBody Disease disease,
                                       @RequestParam(required = false) Long reporterId) {
        try {
            Disease created = diseaseService.create(disease, reporterId);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
