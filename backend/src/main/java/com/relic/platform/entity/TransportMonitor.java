package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transport_monitor")
public class TransportMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transport_id", nullable = false)
    private Long transportId;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "humidity", precision = 5, scale = 2)
    private BigDecimal humidity;

    @Column(name = "vibration", precision = 8, scale = 4)
    private BigDecimal vibration;

    @Column(name = "gps_location", length = 128)
    private String gpsLocation;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
