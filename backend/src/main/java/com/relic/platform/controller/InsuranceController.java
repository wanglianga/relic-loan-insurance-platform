package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.Insurance;
import com.relic.platform.service.InsuranceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insurances")
public class InsuranceController {

    @Autowired
    private InsuranceService insuranceService;

    @GetMapping("/")
    public ApiResponse<PageResult<Insurance>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        try {
            Page<Insurance> insurancePage = insuranceService.list(page, size, status);
            return ApiResponse.success(PageResult.from(insurancePage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Insurance> getById(@PathVariable Long id) {
        try {
            Insurance insurance = insuranceService.getById(id);
            return ApiResponse.success(insurance);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/")
    public ApiResponse<Insurance> create(@RequestBody Insurance insurance) {
        try {
            Insurance created = insuranceService.create(insurance);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/verify")
    public ApiResponse<Insurance> verify(@PathVariable Long id,
                                         @RequestBody Insurance insurance,
                                         @RequestParam(required = false) Long verifierId) {
        try {
            Insurance verified = insuranceService.verify(id, insurance, verifierId);
            return ApiResponse.success(verified);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/activate")
    public ApiResponse<Insurance> activate(@PathVariable Long id) {
        try {
            Insurance activated = insuranceService.activate(id);
            return ApiResponse.success(activated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
