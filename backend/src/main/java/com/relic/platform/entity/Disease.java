package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "disease")
public class Disease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    @Column(name = "disease_type", nullable = false, length = 32)
    private String diseaseType;

    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", length = 256)
    private String location;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "annotation_x", precision = 8, scale = 4)
    private BigDecimal annotationX;

    @Column(name = "annotation_y", precision = 8, scale = 4)
    private BigDecimal annotationY;

    @Column(name = "annotation_width", precision = 8, scale = 4)
    private BigDecimal annotationWidth;

    @Column(name = "annotation_height", precision = 8, scale = 4)
    private BigDecimal annotationHeight;

    @Column(name = "hd_photo_url", length = 512)
    private String hdPhotoUrl;

    @Column(name = "review_status", length = 32)
    private String reviewStatus;

    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Transient
    private String reporterName;

    @Transient
    private DiseaseReview latestReview;
}
