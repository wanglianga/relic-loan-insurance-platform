package com.relic.platform.service;

import com.relic.platform.entity.Insurance;
import com.relic.platform.entity.User;
import com.relic.platform.repository.InsuranceRepository;
import com.relic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class InsuranceService {

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Insurance> list(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "verifiedAt"));
        Page<Insurance> insurancePage = insuranceRepository.findByStatus(status, pageable);
        insurancePage.forEach(this::loadVerifierName);
        return insurancePage;
    }

    @Transactional(readOnly = true)
    public Insurance getById(Long id) {
        Insurance insurance = insuranceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("保险记录不存在"));
        loadVerifierName(insurance);
        return insurance;
    }

    private void loadVerifierName(Insurance insurance) {
        if (insurance.getVerifiedBy() != null) {
            Optional<User> verifier = userRepository.findById(insurance.getVerifiedBy());
            verifier.ifPresent(u -> insurance.setVerifierName(u.getName()));
        }
    }

    @Transactional
    public Insurance create(Insurance insurance) {
        insurance.setId(null);
        insurance.setStatus("DRAFT");
        return insuranceRepository.save(insurance);
    }

    @Transactional
    public Insurance verify(Long id, Insurance insurance, Long verifierId) {
        Insurance existing = getById(id);
        if (!"DRAFT".equals(existing.getStatus()) && !"PENDING".equals(existing.getStatus())) {
            throw new RuntimeException("保险记录状态不允许审核");
        }
        existing.setAppraisedValue(insurance.getAppraisedValue());
        existing.setDeductible(insurance.getDeductible());
        existing.setDeductibleClause(insurance.getDeductibleClause());
        existing.setTransportLiability(insurance.getTransportLiability());
        existing.setEffectiveDate(insurance.getEffectiveDate());
        existing.setExpiryDate(insurance.getExpiryDate());
        existing.setVerifiedBy(verifierId);
        existing.setVerifiedAt(LocalDateTime.now());
        existing.setStatus("VERIFIED");
        return insuranceRepository.save(existing);
    }

    @Transactional
    public Insurance activate(Long id) {
        Insurance insurance = getById(id);
        if (!"VERIFIED".equals(insurance.getStatus())) {
            throw new RuntimeException("保险记录状态不允许生效");
        }
        insurance.setStatus("ACTIVE");
        return insuranceRepository.save(insurance);
    }
}
