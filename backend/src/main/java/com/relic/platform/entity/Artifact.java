package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "artifact")
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_code", nullable = false, unique = true, length = 64)
    private String artifactCode;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "era", length = 64)
    private String era;

    @Column(name = "material", length = 64)
    private String material;

    @Column(name = "dimensions", length = 128)
    private String dimensions;

    @Column(name = "source", length = 256)
    private String source;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
