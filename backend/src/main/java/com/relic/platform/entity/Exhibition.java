package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exhibition")
public class Exhibition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "venue", length = 128)
    private String venue;

    @Column(name = "showcase_code", length = 64)
    private String showcaseCode;

    @Column(name = "setup_at")
    private LocalDateTime setupAt;

    @Column(name = "setup_confirmed_by")
    private Long setupConfirmedBy;

    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
