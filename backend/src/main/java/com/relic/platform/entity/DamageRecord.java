package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "damage_record")
public class DamageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id", nullable = false)
    private Long returnId;

    @Column(name = "damage_type", nullable = false, length = 32)
    private String damageType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "responsibility", length = 256)
    private String responsibility;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
