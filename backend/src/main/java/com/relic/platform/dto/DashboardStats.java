package com.relic.platform.dto;

import lombok.Data;

@Data
public class DashboardStats {
    private long artifactTotal;
    private long artifactOnLoan;
    private long artifactUnderRestoration;
    private long artifactRegistered;
    private long loanPending;
    private long loanApproved;
    private long insurancePending;
    private long transportInTransit;
    private long exhibitionActive;
    private long restorationPending;
    private long diseaseTotal;
}
