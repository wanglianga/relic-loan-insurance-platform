package com.relic.platform.service;

import com.relic.platform.entity.Disease;
import com.relic.platform.entity.DiseaseReview;
import com.relic.platform.entity.ExhibitionCondition;
import com.relic.platform.entity.User;
import com.relic.platform.repository.DiseaseRepository;
import com.relic.platform.repository.DiseaseReviewRepository;
import com.relic.platform.repository.ExhibitionConditionRepository;
import com.relic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DiseaseService {

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiseaseReviewRepository diseaseReviewRepository;

    @Autowired
    private ExhibitionConditionRepository exhibitionConditionRepository;

    @Transactional(readOnly = true)
    public Page<Disease> list(int page, int size, String reviewStatus) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"));
        Page<Disease> diseasePage;
        if (reviewStatus != null && !reviewStatus.isEmpty()) {
            diseasePage = diseaseRepository.findAll((root, query, cb) -> 
                cb.equal(root.get("reviewStatus"), reviewStatus), pageable);
        } else {
            diseasePage = diseaseRepository.findAll(pageable);
        }
        diseasePage.forEach(this::loadReporterName);
        diseasePage.forEach(this::loadLatestReview);
        return diseasePage;
    }

    @Transactional(readOnly = true)
    public List<Disease> listByArtifact(Long artifactId) {
        List<Disease> diseases = diseaseRepository.findByArtifactId(artifactId);
        diseases.forEach(this::loadReporterName);
        diseases.forEach(this::loadLatestReview);
        return diseases;
    }

    @Transactional(readOnly = true)
    public Disease getById(Long id) {
        Disease disease = diseaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("病害记录不存在"));
        loadReporterName(disease);
        loadLatestReview(disease);
        return disease;
    }

    private void loadReporterName(Disease disease) {
        if (disease.getReportedBy() != null) {
            Optional<User> user = userRepository.findById(disease.getReportedBy());
            user.ifPresent(u -> disease.setReporterName(u.getName()));
        }
    }

    private void loadLatestReview(Disease disease) {
        Optional<DiseaseReview> review = diseaseReviewRepository.findFirstByDiseaseIdOrderByReviewedAtDesc(disease.getId());
        review.ifPresent(r -> {
            loadReviewerName(r);
            loadExhibitionCondition(r);
            disease.setLatestReview(r);
        });
    }

    private void loadReviewerName(DiseaseReview review) {
        if (review.getReviewerId() != null) {
            Optional<User> user = userRepository.findById(review.getReviewerId());
            user.ifPresent(u -> review.setReviewerName(u.getName()));
        }
    }

    private void loadExhibitionCondition(DiseaseReview review) {
        if (review.getId() != null) {
            Optional<ExhibitionCondition> condition = exhibitionConditionRepository.findByDiseaseReviewId(review.getId());
            condition.ifPresent(review::setExhibitionCondition);
        }
    }

    @Transactional
    public Disease create(Disease disease, Long reporterId) {
        disease.setId(null);
        disease.setReportedBy(reporterId);
        disease.setReportedAt(LocalDateTime.now());
        if (disease.getReviewStatus() == null) {
            disease.setReviewStatus("PENDING");
        }
        return diseaseRepository.save(disease);
    }

    @Transactional
    public Disease updateAnnotation(Long id, BigDecimal annotationX, BigDecimal annotationY, 
                                    BigDecimal annotationWidth, BigDecimal annotationHeight, String hdPhotoUrl) {
        Disease disease = getById(id);
        disease.setAnnotationX(annotationX);
        disease.setAnnotationY(annotationY);
        disease.setAnnotationWidth(annotationWidth);
        disease.setAnnotationHeight(annotationHeight);
        if (hdPhotoUrl != null) {
            disease.setHdPhotoUrl(hdPhotoUrl);
        }
        return diseaseRepository.save(disease);
    }

    @Transactional
    public DiseaseReview reviewDisease(Long diseaseId, Long loanId, Boolean affectsLoanGrade, 
                                       String loanGradeImpact, String reviewNotes, Long reviewerId) {
        Disease disease = getById(diseaseId);
        
        DiseaseReview review = new DiseaseReview();
        review.setDiseaseId(diseaseId);
        review.setLoanId(loanId);
        review.setAffectsLoanGrade(affectsLoanGrade);
        review.setLoanGradeImpact(loanGradeImpact);
        review.setReviewNotes(reviewNotes);
        review.setReviewerId(reviewerId);
        review.setReviewedAt(LocalDateTime.now());
        
        DiseaseReview savedReview = diseaseReviewRepository.save(review);
        savedReview.setDiseaseType(disease.getDiseaseType());
        
        if (Boolean.TRUE.equals(affectsLoanGrade)) {
            disease.setReviewStatus("REVIEWED_WITH_IMPACT");
        } else {
            disease.setReviewStatus("REVIEWED");
        }
        diseaseRepository.save(disease);
        
        loadReviewerName(savedReview);
        return savedReview;
    }

    @Transactional
    public ExhibitionCondition generateExhibitionCondition(Long diseaseReviewId, Long loanId) {
        DiseaseReview review = diseaseReviewRepository.findById(diseaseReviewId)
                .orElseThrow(() -> new RuntimeException("病害复核记录不存在"));
        
        Disease disease = getById(review.getDiseaseId());
        
        ExhibitionCondition condition = new ExhibitionCondition();
        condition.setLoanId(loanId);
        condition.setDiseaseReviewId(diseaseReviewId);
        condition.setCreatedAt(LocalDateTime.now());
        
        String diseaseType = disease.getDiseaseType();
        String severity = disease.getSeverity();
        
        switch (diseaseType) {
            case "GLAZE_CRACKING":
                condition.setDisplayDistanceMin(new BigDecimal("1.5"));
                condition.setIlluminanceLimit(new BigDecimal("50"));
                condition.setBracketRequirements("使用减震支架，底部垫3mm厚硅胶缓冲垫");
                condition.setInsuranceAdditionalTerms("釉面开裂扩展险，保额增加20%");
                break;
            case "PAINT_PEELING":
                condition.setDisplayDistanceMin(new BigDecimal("2.0"));
                condition.setIlluminanceLimit(new BigDecimal("30"));
                condition.setBracketRequirements("使用无接触悬挂系统，避免触碰彩绘区域");
                condition.setInsuranceAdditionalTerms("彩绘脱落意外险，保额增加30%");
                break;
            case "WOOD_DEFORMATION":
                condition.setDisplayDistanceMin(new BigDecimal("1.0"));
                condition.setIlluminanceLimit(new BigDecimal("50"));
                condition.setBracketRequirements("定制承托支架，均匀受力，接触面积不小于60%");
                condition.setInsuranceAdditionalTerms("木胎变形扩展险，保额增加25%");
                break;
            case "METAL_CORROSION":
                condition.setDisplayDistanceMin(new BigDecimal("1.5"));
                condition.setIlluminanceLimit(new BigDecimal("100"));
                condition.setBracketRequirements("使用惰性材料支架，避免电化学腐蚀");
                condition.setInsuranceAdditionalTerms("金属锈蚀扩展险，保额增加20%");
                break;
            default:
                condition.setDisplayDistanceMin(new BigDecimal("1.0"));
                condition.setIlluminanceLimit(new BigDecimal("100"));
                condition.setBracketRequirements("标准展示支架");
                condition.setInsuranceAdditionalTerms("无特殊附加条件");
        }
        
        if ("SEVERE".equals(severity)) {
            condition.setDisplayDistanceMin(condition.getDisplayDistanceMin().add(new BigDecimal("0.5")));
            condition.setIlluminanceLimit(condition.getIlluminanceLimit().multiply(new BigDecimal("0.7")));
            condition.setOtherRequirements("重度病害展品，需每日巡检并记录状态");
        } else if ("MODERATE".equals(severity)) {
            condition.setOtherRequirements("中度病害展品，需每三日巡检并记录状态");
        } else {
            condition.setOtherRequirements("轻度病害展品，需每周巡检并记录状态");
        }
        
        return exhibitionConditionRepository.save(condition);
    }

    @Transactional(readOnly = true)
    public List<DiseaseReview> listReviewsByDisease(Long diseaseId) {
        List<DiseaseReview> reviews = diseaseReviewRepository.findByDiseaseIdOrderByReviewedAtDesc(diseaseId);
        reviews.forEach(this::loadReviewerName);
        reviews.forEach(this::loadExhibitionCondition);
        reviews.forEach(r -> {
            if (r.getDiseaseId() != null) {
                Optional<Disease> disease = diseaseRepository.findById(r.getDiseaseId());
                disease.ifPresent(d -> r.setDiseaseType(d.getDiseaseType()));
            }
        });
        return reviews;
    }

    @Transactional(readOnly = true)
    public List<DiseaseReview> listReviewsByLoan(Long loanId) {
        List<DiseaseReview> reviews = diseaseReviewRepository.findByLoanId(loanId);
        reviews.forEach(this::loadReviewerName);
        reviews.forEach(this::loadExhibitionCondition);
        reviews.forEach(r -> {
            if (r.getDiseaseId() != null) {
                Optional<Disease> disease = diseaseRepository.findById(r.getDiseaseId());
                disease.ifPresent(d -> {
                    r.setDisease(d);
                    r.setDiseaseType(d.getDiseaseType());
                });
            }
        });
        return reviews;
    }

    @Transactional(readOnly = true)
    public List<ExhibitionCondition> listExhibitionConditionsByLoan(Long loanId) {
        return exhibitionConditionRepository.findByLoanId(loanId);
    }
}
