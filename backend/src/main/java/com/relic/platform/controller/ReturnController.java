package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.DamageRecord;
import com.relic.platform.entity.ReturnRecord;
import com.relic.platform.service.ReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    @Autowired
    private ReturnService returnService;

    @GetMapping("/")
    public ApiResponse<PageResult<ReturnRecord>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        try {
            Page<ReturnRecord> returnPage = returnService.list(page, size, status);
            return ApiResponse.success(PageResult.from(returnPage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<ReturnRecord> getById(@PathVariable Long id) {
        try {
            ReturnRecord returnRecord = returnService.getById(id);
            return ApiResponse.success(returnRecord);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/")
    public ApiResponse<ReturnRecord> create(@RequestBody ReturnRecord record) {
        try {
            ReturnRecord created = returnService.create(record);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/receive")
    public ApiResponse<ReturnRecord> receive(@PathVariable Long id,
                                             @RequestBody ReturnRecord record,
                                             @RequestParam(required = false) Long receiverId) {
        try {
            ReturnRecord received = returnService.receive(id, record, receiverId);
            return ApiResponse.success(received);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/damages")
    public ApiResponse<DamageRecord> addDamage(@PathVariable Long id,
                                               @RequestBody DamageRecord damage) {
        try {
            DamageRecord created = returnService.addDamage(id, damage);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
