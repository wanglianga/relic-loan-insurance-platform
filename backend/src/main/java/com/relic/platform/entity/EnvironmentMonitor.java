package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "environment_monitor")
public class EnvironmentMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "humidity", precision = 5, scale = 2)
    private BigDecimal humidity;

    @Column(name = "illuminance", precision = 8, scale = 2)
    private BigDecimal illuminance;

    @Column(name = "vibration", precision = 8, scale = 4)
    private BigDecimal vibration;

    @Column(name = "is_alert")
    private Boolean isAlert;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
