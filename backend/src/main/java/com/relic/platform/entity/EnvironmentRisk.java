package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "environment_risk")
public class EnvironmentRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "environment_precheck_id", nullable = false)
    private Long environmentPrecheckId;

    @Column(name = "risk_level", nullable = false, length = 32)
    private String riskLevel;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Column(name = "mitigation_suggestions", columnDefinition = "TEXT")
    private String mitigationSuggestions;

    @Column(name = "requires_approval")
    private Boolean requiresApproval;

    @Column(name = "showcase_suggestion", columnDefinition = "TEXT")
    private String showcaseSuggestion;

    @Column(name = "exhibition_duration_suggestion")
    private Integer exhibitionDurationSuggestion;

    @Column(name = "monitoring_equipment", columnDefinition = "TEXT")
    private String monitoringEquipment;

    @Column(name = "assessed_at")
    private LocalDateTime assessedAt;
}
