package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "return_record")
public class ReturnRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "return_transport_id")
    private Long returnTransportId;

    @Column(name = "overall_status", nullable = false, length = 32)
    private String overallStatus;

    @Column(name = "return_notes", columnDefinition = "TEXT")
    private String returnNotes;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;
}
