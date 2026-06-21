package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.Artifact;
import com.relic.platform.entity.ArtifactRestriction;
import com.relic.platform.service.ArtifactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artifacts")
public class ArtifactController {

    @Autowired
    private ArtifactService artifactService;

    @GetMapping("")
    public ApiResponse<PageResult<Artifact>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            Page<Artifact> artifactPage = artifactService.list(page, size, status, keyword);
            return ApiResponse.success(PageResult.from(artifactPage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Artifact> getById(@PathVariable Long id) {
        try {
            Artifact artifact = artifactService.getById(id);
            return ApiResponse.success(artifact);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("")
    public ApiResponse<Artifact> create(@RequestBody Artifact artifact) {
        try {
            Artifact created = artifactService.create(artifact);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Artifact> update(@PathVariable Long id, @RequestBody Artifact artifact) {
        try {
            Artifact updated = artifactService.update(id, artifact);
            return ApiResponse.success(updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/restriction")
    public ApiResponse<ArtifactRestriction> updateRestriction(
            @PathVariable Long id,
            @RequestBody ArtifactRestriction restriction) {
        try {
            ArtifactRestriction updated = artifactService.updateRestriction(id, restriction);
            return ApiResponse.success(updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
