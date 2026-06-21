package com.relic.platform.service;

import com.relic.platform.dto.DashboardStats;
import com.relic.platform.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    @Autowired
    private ArtifactRepository artifactRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private InsuranceRepository insuranceRepository;
    @Autowired
    private TransportRepository transportRepository;
    @Autowired
    private ExhibitionRepository exhibitionRepository;
    @Autowired
    private RestorationRepository restorationRepository;
    @Autowired
    private DiseaseRepository diseaseRepository;

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();
        stats.setArtifactTotal(artifactRepository.count());
        stats.setArtifactOnLoan(artifactRepository.countByStatus("ON_LOAN"));
        stats.setArtifactUnderRestoration(artifactRepository.countByStatus("UNDER_RESTORATION"));
        stats.setArtifactRegistered(artifactRepository.countByStatus("REGISTERED"));
        stats.setLoanPending(loanRepository.countByStatus("PENDING"));
        stats.setLoanApproved(loanRepository.countByStatus("APPROVED"));
        stats.setInsurancePending(insuranceRepository.countByStatus("PENDING"));
        stats.setTransportInTransit(transportRepository.countByStatus("IN_TRANSIT"));
        stats.setExhibitionActive(exhibitionRepository.countByStatus("ACTIVE"));
        stats.setRestorationPending(restorationRepository.countByStatus("PROPOSED") + restorationRepository.countByStatus("APPROVED"));
        stats.setDiseaseTotal(diseaseRepository.count());
        return stats;
    }
}
