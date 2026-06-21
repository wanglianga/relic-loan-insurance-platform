package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "restoration")
public class Restoration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "plan", columnDefinition = "TEXT")
    private String plan;

    @Column(name = "cleaning_method", length = 128)
    private String cleaningMethod;

    @Column(name = "reinforcement_material", length = 128)
    private String reinforcementMaterial;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "proposed_by", nullable = false)
    private Long proposedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "proposed_at")
    private LocalDateTime proposedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Transient
    private List<RestorationStep> steps;
    @Transient
    private String proposerName;
    @Transient
    private String approverName;
    @Transient
    private String artifactName;
}
