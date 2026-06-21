package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.Loan;
import com.relic.platform.entity.LoanEnvironment;
import com.relic.platform.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @GetMapping("")
    public ApiResponse<PageResult<Loan>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long applicantId) {
        try {
            Page<Loan> loanPage = loanService.list(page, size, status, applicantId);
            return ApiResponse.success(PageResult.from(loanPage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Loan> getById(@PathVariable Long id) {
        try {
            Loan loan = loanService.getById(id);
            return ApiResponse.success(loan);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("")
    public ApiResponse<Loan> create(@RequestBody Loan loan,
                                    @RequestParam(required = false) Long applicantId) {
        try {
            Loan created = loanService.create(loan, applicantId);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<Loan> approve(@PathVariable Long id,
                                     @RequestParam(required = false) Long approverId) {
        try {
            Loan approved = loanService.approve(id, approverId);
            return ApiResponse.success(approved);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<Loan> reject(@PathVariable Long id,
                                    @RequestBody Map<String, String> body) {
        try {
            String rejectionReason = body.get("rejectionReason");
            Loan rejected = loanService.reject(id, rejectionReason);
            return ApiResponse.success(rejected);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/environment")
    public ApiResponse<LoanEnvironment> updateEnvironment(@PathVariable Long id,
                                                          @RequestBody LoanEnvironment env) {
        try {
            LoanEnvironment updated = loanService.updateEnvironment(id, env);
            return ApiResponse.success(updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
