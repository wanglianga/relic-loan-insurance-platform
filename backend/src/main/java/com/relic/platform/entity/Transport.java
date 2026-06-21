package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transport")
public class Transport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "packaging_standard", length = 128)
    private String packagingStandard;

    @Column(name = "box_code", length = 64)
    private String boxCode;

    @Column(name = "box_spec", length = 128)
    private String boxSpec;

    @Column(name = "inner_material", length = 128)
    private String innerMaterial;

    @Column(name = "seal_code", length = 64)
    private String sealCode;

    @Column(name = "sealed_at")
    private LocalDateTime sealedAt;

    @Column(name = "sealed_by")
    private Long sealedBy;

    @Column(name = "escort_id")
    private Long escortId;

    @Column(name = "route", length = 512)
    private String route;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "departed_at")
    private LocalDateTime departedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;
}
