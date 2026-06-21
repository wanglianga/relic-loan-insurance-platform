package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "restoration_step")
public class RestorationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restoration_id", nullable = false)
    private Long restorationId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "material_used", length = 128)
    private String materialUsed;

    @Column(name = "before_photo_url", length = 512)
    private String beforePhotoUrl;

    @Column(name = "after_photo_url", length = 512)
    private String afterPhotoUrl;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operated_at")
    private LocalDateTime operatedAt;
}
