package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.PageResult;
import com.relic.platform.entity.Disease;
import com.relic.platform.entity.DiseaseReview;
import com.relic.platform.entity.ExhibitionCondition;
import com.relic.platform.service.DiseaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diseases")
public class DiseaseController {

    @Autowired
    private DiseaseService diseaseService;

    @GetMapping("")
    public ApiResponse<PageResult<Disease>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String reviewStatus) {
        try {
            Page<Disease> diseasePage = diseaseService.list(page, size, reviewStatus);
            return ApiResponse.success(PageResult.from(diseasePage));
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Disease> getById(@PathVariable Long id) {
        try {
            Disease disease = diseaseService.getById(id);
            return ApiResponse.success(disease);
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

    @PostMapping("")
    public ApiResponse<Disease> create(@RequestBody Disease disease,
                                       @RequestParam(required = false) Long reporterId) {
        try {
            Disease created = diseaseService.create(disease, reporterId);
            return ApiResponse.success(created);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/annotation")
    public ApiResponse<Disease> updateAnnotation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            BigDecimal annotationX = body.get("annotationX") != null ? 
                new BigDecimal(body.get("annotationX").toString()) : null;
            BigDecimal annotationY = body.get("annotationY") != null ? 
                new BigDecimal(body.get("annotationY").toString()) : null;
            BigDecimal annotationWidth = body.get("annotationWidth") != null ? 
                new BigDecimal(body.get("annotationWidth").toString()) : null;
            BigDecimal annotationHeight = body.get("annotationHeight") != null ? 
                new BigDecimal(body.get("annotationHeight").toString()) : null;
            String hdPhotoUrl = body.get("hdPhotoUrl") != null ? 
                body.get("hdPhotoUrl").toString() : null;
            
            Disease updated = diseaseService.updateAnnotation(
                id, annotationX, annotationY, annotationWidth, annotationHeight, hdPhotoUrl);
            return ApiResponse.success(updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/review")
    public ApiResponse<DiseaseReview> reviewDisease(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestParam(required = false) Long reviewerId) {
        try {
            Long loanId = body.get("loanId") != null ? 
                Long.valueOf(body.get("loanId").toString()) : null;
            Boolean affectsLoanGrade = body.get("affectsLoanGrade") != null ? 
                Boolean.valueOf(body.get("affectsLoanGrade").toString()) : false;
            String loanGradeImpact = body.get("loanGradeImpact") != null ? 
                body.get("loanGradeImpact").toString() : null;
            String reviewNotes = body.get("reviewNotes") != null ? 
                body.get("reviewNotes").toString() : null;
            
            DiseaseReview review = diseaseService.reviewDisease(
                id, loanId, affectsLoanGrade, loanGradeImpact, reviewNotes, reviewerId);
            return ApiResponse.success(review);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/reviews/{reviewId}/exhibition-condition")
    public ApiResponse<ExhibitionCondition> generateExhibitionCondition(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> body) {
        try {
            Long loanId = Long.valueOf(body.get("loanId").toString());
            ExhibitionCondition condition = diseaseService.generateExhibitionCondition(reviewId, loanId);
            return ApiResponse.success(condition);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<DiseaseReview>> listReviewsByDisease(@PathVariable Long id) {
        try {
            List<DiseaseReview> reviews = diseaseService.listReviewsByDisease(id);
            return ApiResponse.success(reviews);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/loan/{loanId}/reviews")
    public ApiResponse<List<DiseaseReview>> listReviewsByLoan(@PathVariable Long loanId) {
        try {
            List<DiseaseReview> reviews = diseaseService.listReviewsByLoan(loanId);
            return ApiResponse.success(reviews);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/loan/{loanId}/exhibition-conditions")
    public ApiResponse<List<ExhibitionCondition>> listExhibitionConditionsByLoan(@PathVariable Long loanId) {
        try {
            List<ExhibitionCondition> conditions = diseaseService.listExhibitionConditionsByLoan(loanId);
            return ApiResponse.success(conditions);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
