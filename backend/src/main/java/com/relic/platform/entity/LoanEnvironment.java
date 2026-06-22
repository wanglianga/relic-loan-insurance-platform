package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "loan_environment")
public class LoanEnvironment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin;

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax;

    @Column(name = "temperature_fluctuation", precision = 5, scale = 2)
    private BigDecimal temperatureFluctuation;

    @Column(name = "humidity_min", precision = 5, scale = 2)
    private BigDecimal humidityMin;

    @Column(name = "humidity_max", precision = 5, scale = 2)
    private BigDecimal humidityMax;

    @Column(name = "humidity_fluctuation", precision = 5, scale = 2)
    private BigDecimal humidityFluctuation;

    @Column(name = "illuminance_max", precision = 8, scale = 2)
    private BigDecimal illuminanceMax;

    @Column(name = "vibration_max", precision = 8, scale = 4)
    private BigDecimal vibrationMax;

    @Column(name = "security_route", columnDefinition = "TEXT")
    private String securityRoute;

    @Column(name = "lighting_points", columnDefinition = "TEXT")
    private String lightingPoints;

    @Column(name = "visitor_route", columnDefinition = "TEXT")
    private String visitorRoute;

    @Column(name = "patrol_schedule", columnDefinition = "TEXT")
    private String patrolSchedule;

    @Column(name = "continuous_temperature_data", columnDefinition = "TEXT")
    private String continuousTemperatureData;

    @Column(name = "continuous_humidity_data", columnDefinition = "TEXT")
    private String continuousHumidityData;

    @Column(name = "setup_date")
    private LocalDate setupDate;
}
