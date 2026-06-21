package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.Transport;
import com.relic.platform.entity.TransportMonitor;
import com.relic.platform.service.TransportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transports")
public class TransportController {

    @Autowired
    private TransportService transportService;

    @GetMapping("/")
    public ApiResponse<PageResult<Transport>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        try {
            Page<Transport> transportPage = transportService.list(page, size, status);
            return ApiResponse.success(PageResult.from(transportPage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Transport> getById(@PathVariable Long id) {
        try {
            Transport transport = transportService.getById(id);
            return ApiResponse.success(transport);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/")
    public ApiResponse<Transport> create(@RequestBody Transport transport) {
        try {
            Transport created = transportService.create(transport);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/seal")
    public ApiResponse<Transport> seal(@PathVariable Long id,
                                       @RequestBody Transport transport,
                                       @RequestParam(required = false) Long sealerId) {
        try {
            Transport sealed = transportService.seal(id, transport, sealerId);
            return ApiResponse.success(sealed);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/depart")
    public ApiResponse<Transport> depart(@PathVariable Long id) {
        try {
            Transport departed = transportService.depart(id);
            return ApiResponse.success(departed);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/arrive")
    public ApiResponse<Transport> arrive(@PathVariable Long id) {
        try {
            Transport arrived = transportService.arrive(id);
            return ApiResponse.success(arrived);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/monitors")
    public ApiResponse<TransportMonitor> addMonitor(@PathVariable Long id,
                                                    @RequestBody TransportMonitor monitor) {
        try {
            TransportMonitor created = transportService.addMonitor(id, monitor);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
