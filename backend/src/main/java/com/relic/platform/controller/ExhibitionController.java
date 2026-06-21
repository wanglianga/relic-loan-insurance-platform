package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.EnvironmentMonitor;
import com.relic.platform.entity.Exhibition;
import com.relic.platform.service.ExhibitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exhibitions")
public class ExhibitionController {

    @Autowired
    private ExhibitionService exhibitionService;

    @GetMapping("/")
    public ApiResponse<PageResult<Exhibition>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        try {
            Page<Exhibition> exhibitionPage = exhibitionService.list(page, size, status);
            return ApiResponse.success(PageResult.from(exhibitionPage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Exhibition> getById(@PathVariable Long id) {
        try {
            Exhibition exhibition = exhibitionService.getById(id);
            return ApiResponse.success(exhibition);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/")
    public ApiResponse<Exhibition> create(@RequestBody Exhibition exhibition) {
        try {
            Exhibition created = exhibitionService.setup(exhibition);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/confirm-setup")
    public ApiResponse<Exhibition> confirmSetup(@PathVariable Long id,
                                                @RequestParam(required = false) Long confirmerId) {
        try {
            Exhibition confirmed = exhibitionService.confirmSetup(id, confirmerId);
            return ApiResponse.success(confirmed);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/environment-monitors")
    public ApiResponse<EnvironmentMonitor> addEnvironmentMonitor(
            @PathVariable Long id,
            @RequestBody EnvironmentMonitor monitor) {
        try {
            EnvironmentMonitor created = exhibitionService.addEnvironmentMonitor(id, monitor);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
