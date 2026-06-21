package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.relic.platform.entity.LoanEnvironment;
import com.relic.platform.entity.Insurance;
import com.relic.platform.entity.Transport;
import com.relic.platform.entity.Exhibition;
import com.relic.platform.entity.ReturnRecord;

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

    @Transient
    private String applicantName;
    @Transient
    private String artifactName;
    @Transient
    private String artifactCode;
    @Transient
    private LoanEnvironment environment;
    @Transient
    private Insurance insurance;
    @Transient
    private Transport transport;
    @Transient
    private Exhibition exhibition;
    @Transient
    private ReturnRecord returnRecord;
}
