package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exhibition_condition")
public class ExhibitionCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "disease_review_id")
    private Long diseaseReviewId;

    @Column(name = "display_distance_min", precision = 6, scale = 2)
    private BigDecimal displayDistanceMin;

    @Column(name = "illuminance_limit", precision = 8, scale = 2)
    private BigDecimal illuminanceLimit;

    @Column(name = "bracket_requirements", columnDefinition = "TEXT")
    private String bracketRequirements;

    @Column(name = "insurance_additional_terms", columnDefinition = "TEXT")
    private String insuranceAdditionalTerms;

    @Column(name = "other_requirements", columnDefinition = "TEXT")
    private String otherRequirements;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
