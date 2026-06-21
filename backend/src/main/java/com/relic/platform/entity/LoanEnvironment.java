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

    @Column(name = "humidity_min", precision = 5, scale = 2)
    private BigDecimal humidityMin;

    @Column(name = "humidity_max", precision = 5, scale = 2)
    private BigDecimal humidityMax;

    @Column(name = "illuminance_max", precision = 8, scale = 2)
    private BigDecimal illuminanceMax;

    @Column(name = "vibration_max", precision = 8, scale = 4)
    private BigDecimal vibrationMax;

    @Column(name = "security_route", columnDefinition = "TEXT")
    private String securityRoute;

    @Column(name = "setup_date")
    private LocalDate setupDate;
}
