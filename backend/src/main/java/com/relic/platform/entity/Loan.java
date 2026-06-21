package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "borrowing_institution", nullable = false, length = 128)
    private String borrowingInstitution;

    @Column(name = "exhibition_name", nullable = false, length = 128)
    private String exhibitionName;

    @Column(name = "loan_start_date", nullable = false)
    private LocalDate loanStartDate;

    @Column(name = "loan_end_date", nullable = false)
    private LocalDate loanEndDate;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
