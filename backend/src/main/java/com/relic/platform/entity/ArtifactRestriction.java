package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "artifact_restriction")
public class ArtifactRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    @Column(name = "no_exhibition")
    private Boolean noExhibition;

    @Column(name = "no_exhibition_reason", columnDefinition = "TEXT")
    private String noExhibitionReason;

    @Column(name = "no_loan")
    private Boolean noLoan;

    @Column(name = "no_loan_reason", columnDefinition = "TEXT")
    private String noLoanReason;

    @Column(name = "min_loan_interval_days")
    private Integer minLoanIntervalDays;

    @Column(name = "max_loan_days")
    private Integer maxLoanDays;

    @Column(name = "loan_conditions", columnDefinition = "TEXT")
    private String loanConditions;
}
