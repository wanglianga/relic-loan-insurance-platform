package com.relic.platform.service;

import com.relic.platform.entity.*;
import com.relic.platform.repository.*;
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
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanEnvironmentRepository loanEnvironmentRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private TransportRepository transportRepository;

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @Autowired
    private ReturnRecordRepository returnRecordRepository;

    @Transactional(readOnly = true)
    public Page<Loan> list(int page, int size, String status, Long applicantId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Loan> loanPage = loanRepository.findByConditions(status, applicantId, pageable);
        loanPage.forEach(this::loadBasicRelations);
        return loanPage;
    }

    @Transactional(readOnly = true)
    public Loan getById(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("借展申请不存在"));
        loadBasicRelations(loan);
        loadDetailRelations(loan);
        return loan;
    }

    private void loadBasicRelations(Loan loan) {
        if (loan.getApplicantId() != null) {
            Optional<User> applicant = userRepository.findById(loan.getApplicantId());
            applicant.ifPresent(u -> loan.setApplicantName(u.getName()));
        }
        if (loan.getArtifactId() != null) {
            Optional<Artifact> artifact = artifactRepository.findById(loan.getArtifactId());
            artifact.ifPresent(a -> {
                loan.setArtifactName(a.getName());
                loan.setArtifactCode(a.getArtifactCode());
            });
        }
    }

    private void loadDetailRelations(Loan loan) {
        Optional<LoanEnvironment> environment = loanEnvironmentRepository.findByLoanId(loan.getId());
        environment.ifPresent(loan::setEnvironment);

        Optional<Insurance> insurance = insuranceRepository.findByLoanId(loan.getId());
        insurance.ifPresent(loan::setInsurance);

        Optional<Transport> transport = transportRepository.findByLoanId(loan.getId());
        transport.ifPresent(loan::setTransport);

        Optional<Exhibition> exhibition = exhibitionRepository.findByLoanId(loan.getId());
        exhibition.ifPresent(loan::setExhibition);

        Optional<ReturnRecord> returnRecord = returnRecordRepository.findByLoanId(loan.getId());
        returnRecord.ifPresent(loan::setReturnRecord);
    }

    @Transactional
    public Loan create(Loan loan, Long applicantId) {
        if (!artifactRepository.existsById(loan.getArtifactId())) {
            throw new RuntimeException("藏品不存在");
        }
        loan.setId(null);
        loan.setApplicantId(applicantId);
        loan.setStatus("PENDING");
        loan.setCreatedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    @Transactional
    public Loan approve(Long id, Long approverId) {
        Loan loan = getById(id);
        if (!"PENDING".equals(loan.getStatus())) {
            throw new RuntimeException("借展申请状态不允许审批");
        }
        loan.setStatus("APPROVED");
        loan.setApprovedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    @Transactional
    public Loan reject(Long id, String reason) {
        Loan loan = getById(id);
        if (!"PENDING".equals(loan.getStatus())) {
            throw new RuntimeException("借展申请状态不允许拒绝");
        }
        loan.setStatus("REJECTED");
        loan.setRejectionReason(reason);
        return loanRepository.save(loan);
    }

    @Transactional
    public LoanEnvironment updateEnvironment(Long loanId, LoanEnvironment env) {
        Loan loan = getById(loanId);
        LoanEnvironment existing = loanEnvironmentRepository.findByLoanId(loanId)
                .orElse(null);

        if (existing == null) {
            env.setId(null);
            env.setLoanId(loanId);
            return loanEnvironmentRepository.save(env);
        }

        existing.setTemperatureMin(env.getTemperatureMin());
        existing.setTemperatureMax(env.getTemperatureMax());
        existing.setHumidityMin(env.getHumidityMin());
        existing.setHumidityMax(env.getHumidityMax());
        existing.setIlluminanceMax(env.getIlluminanceMax());
        existing.setVibrationMax(env.getVibrationMax());
        existing.setSecurityRoute(env.getSecurityRoute());
        existing.setSetupDate(env.getSetupDate());
        return loanEnvironmentRepository.save(existing);
    }
}
