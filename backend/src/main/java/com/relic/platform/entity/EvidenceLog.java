package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "evidence_log")
public class EvidenceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "from_status", length = 32)
    private String fromStatus;

    @Column(name = "to_status", length = 32)
    private String toStatus;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "responsibility_impact", length = 256)
    private String responsibilityImpact;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "related_entity_type", length = 32)
    private String relatedEntityType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
