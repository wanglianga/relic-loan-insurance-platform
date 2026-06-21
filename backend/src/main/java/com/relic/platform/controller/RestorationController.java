package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.Restoration;
import com.relic.platform.entity.RestorationStep;
import com.relic.platform.service.RestorationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restorations")
public class RestorationController {

    @Autowired
    private RestorationService restorationService;

    @GetMapping("/")
    public ApiResponse<PageResult<Restoration>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        try {
            Page<Restoration> restorationPage = restorationService.list(page, size, status);
            return ApiResponse.success(PageResult.from(restorationPage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Restoration> getById(@PathVariable Long id) {
        try {
            Restoration restoration = restorationService.getById(id);
            return ApiResponse.success(restoration);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/")
    public ApiResponse<Restoration> create(@RequestBody Restoration restoration,
                                           @RequestParam(required = false) Long proposerId) {
        try {
            Restoration created = restorationService.create(restoration, proposerId);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<Restoration> approve(@PathVariable Long id,
                                            @RequestParam(required = false) Long approverId) {
        try {
            Restoration approved = restorationService.approve(id, approverId);
            return ApiResponse.success(approved);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<Restoration> complete(@PathVariable Long id) {
        try {
            Restoration completed = restorationService.complete(id);
            return ApiResponse.success(completed);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/steps")
    public ApiResponse<RestorationStep> addStep(@PathVariable Long id,
                                                @RequestBody RestorationStep step,
                                                @RequestParam(required = false) Long operatorId) {
        try {
            RestorationStep created = restorationService.addStep(id, step, operatorId);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
