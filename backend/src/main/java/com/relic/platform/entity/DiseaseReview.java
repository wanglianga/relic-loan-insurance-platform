package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "disease_review")
public class DiseaseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disease_id", nullable = false)
    private Long diseaseId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "affects_loan_grade")
    private Boolean affectsLoanGrade;

    @Column(name = "loan_grade_impact", columnDefinition = "TEXT")
    private String loanGradeImpact;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Transient
    private String reviewerName;

    @Transient
    private String diseaseType;

    @Transient
    private Disease disease;

    @Transient
    private ExhibitionCondition exhibitionCondition;
}
