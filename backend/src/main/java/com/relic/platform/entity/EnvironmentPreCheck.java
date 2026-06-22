package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "environment_precheck")
public class EnvironmentPreCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "temperature_data", columnDefinition = "TEXT")
    private String temperatureData;

    @Column(name = "humidity_data", columnDefinition = "TEXT")
    private String humidityData;

    @Column(name = "lighting_layout", columnDefinition = "TEXT")
    private String lightingLayout;

    @Column(name = "visitor_flow", columnDefinition = "TEXT")
    private String visitorFlow;

    @Column(name = "security_patrol_plan", columnDefinition = "TEXT")
    private String securityPatrolPlan;

    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Transient
    private String submitterName;

    @Transient
    private EnvironmentRisk latestRisk;

    public LocalDateTime getCreatedAt() {
        return this.submittedAt;
    }
}
