package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "insurance")
public class Insurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "appraised_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal appraisedValue;

    @Column(name = "deductible", precision = 14, scale = 2)
    private BigDecimal deductible;

    @Column(name = "deductible_clause", columnDefinition = "TEXT")
    private String deductibleClause;

    @Column(name = "transport_liability", columnDefinition = "TEXT")
    private String transportLiability;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
