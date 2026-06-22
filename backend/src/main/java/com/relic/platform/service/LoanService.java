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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Autowired
    private EnvironmentPreCheckRepository environmentPreCheckRepository;

    @Autowired
    private EnvironmentRiskRepository environmentRiskRepository;

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
        
        Optional<EnvironmentPreCheck> preCheck = environmentPreCheckRepository.findFirstByLoanIdOrderBySubmittedAtDesc(id);
        if (preCheck.isPresent()) {
            Optional<EnvironmentRisk> risk = environmentRiskRepository.findByEnvironmentPrecheckId(preCheck.get().getId());
            if (risk.isPresent() && Boolean.TRUE.equals(risk.get().getRequiresApproval())) {
                throw new RuntimeException("该申请存在环境风险，需先完成风险 mitigation 后再审批");
            }
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
        existing.setTemperatureFluctuation(env.getTemperatureFluctuation());
        existing.setHumidityMin(env.getHumidityMin());
        existing.setHumidityMax(env.getHumidityMax());
        existing.setHumidityFluctuation(env.getHumidityFluctuation());
        existing.setIlluminanceMax(env.getIlluminanceMax());
        existing.setVibrationMax(env.getVibrationMax());
        existing.setSecurityRoute(env.getSecurityRoute());
        existing.setLightingPoints(env.getLightingPoints());
        existing.setVisitorRoute(env.getVisitorRoute());
        existing.setPatrolSchedule(env.getPatrolSchedule());
        existing.setContinuousTemperatureData(env.getContinuousTemperatureData());
        existing.setContinuousHumidityData(env.getContinuousHumidityData());
        existing.setSetupDate(env.getSetupDate());
        return loanEnvironmentRepository.save(existing);
    }

    @Transactional
    public EnvironmentPreCheck submitEnvironmentPreCheck(Long loanId, EnvironmentPreCheck preCheck, Long submitterId) {
        Loan loan = getById(loanId);
        
        preCheck.setId(null);
        preCheck.setLoanId(loanId);
        preCheck.setSubmittedBy(submitterId);
        preCheck.setSubmittedAt(LocalDateTime.now());
        
        EnvironmentPreCheck saved = environmentPreCheckRepository.save(preCheck);
        
        loadSubmitterName(saved);
        
        Artifact artifact = artifactRepository.findById(loan.getArtifactId()).orElse(null);
        if (artifact != null) {
            EnvironmentRisk risk = assessEnvironmentRisk(saved, artifact);
            saved.setLatestRisk(risk);
        }
        
        return saved;
    }

    @Transactional
    public EnvironmentRisk assessEnvironmentRisk(EnvironmentPreCheck preCheck, Artifact artifact) {
        EnvironmentRisk risk = new EnvironmentRisk();
        risk.setEnvironmentPrecheckId(preCheck.getId());
        risk.setAssessedAt(LocalDateTime.now());
        
        String material = artifact.getMaterial();
        List<String> riskFactors = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        boolean requiresApproval = false;
        String showcaseSuggestion = null;
        Integer durationSuggestion = null;
        List<String> monitoringEquipment = new ArrayList<>();
        
        if (material != null) {
            String materialLower = material.toLowerCase();
            
            if (materialLower.contains("丝") || materialLower.contains("丝绸") || materialLower.contains("纺织")) {
                if (preCheck.getTemperatureData() != null && preCheck.getTemperatureData().contains("high") ||
                    preCheck.getHumidityData() != null && preCheck.getHumidityData().contains("high")) {
                    riskFactors.add("丝织品面临高温高湿环境风险");
                    suggestions.add("需使用恒温恒湿展柜");
                    requiresApproval = true;
                    showcaseSuggestion = "密闭式恒温恒湿展柜，配备除湿系统";
                    monitoringEquipment.add("温湿度实时监测仪");
                }
                
                if (preCheck.getLightingLayout() != null && preCheck.getLightingLayout().contains("high_illuminance")) {
                    riskFactors.add("丝织品面临高照度环境风险，可能导致褪色老化");
                    suggestions.add("需降低照度至50lux以下，使用紫外线过滤玻璃");
                    requiresApproval = true;
                    showcaseSuggestion = showcaseSuggestion != null ? showcaseSuggestion : "低反射紫外线过滤玻璃展柜";
                    durationSuggestion = 30;
                    monitoringEquipment.add("照度计");
                    monitoringEquipment.add("紫外线监测仪");
                }
            }
            
            if (materialLower.contains("漆") || materialLower.contains("漆器")) {
                if (preCheck.getHumidityData() != null && preCheck.getHumidityData().contains("fluctuation")) {
                    riskFactors.add("漆器面临湿度波动风险，可能导致漆层开裂脱落");
                    suggestions.add("需严格控制湿度波动在±5%以内");
                    requiresApproval = true;
                    showcaseSuggestion = "密闭式恒湿展柜，配备精密湿度控制系统";
                    monitoringEquipment.add("高精度温湿度监测仪");
                }
                
                if (preCheck.getVisitorFlow() != null && preCheck.getVisitorFlow().contains("high_traffic")) {
                    riskFactors.add("漆器面临高观众流量风险，振动和微环境波动可能影响器物稳定性");
                    suggestions.add("需设置独立展示区域，限制观众流量");
                    requiresApproval = true;
                    if (showcaseSuggestion == null) {
                        showcaseSuggestion = "减震底座+独立防护围栏";
                    }
                    monitoringEquipment.add("振动监测仪");
                }
            }
            
            if (materialLower.contains("木") || materialLower.contains("木质")) {
                if (preCheck.getHumidityData() != null && preCheck.getHumidityData().contains("dry")) {
                    riskFactors.add("木质文物面临干燥环境风险，可能导致开裂变形");
                    suggestions.add("需增加湿度至50-60%RH");
                    requiresApproval = true;
                    showcaseSuggestion = "加湿型展柜";
                    monitoringEquipment.add("温湿度监测仪");
                }
            }
            
            if (materialLower.contains("金属") || materialLower.contains("铜") || materialLower.contains("铁")) {
                if (preCheck.getHumidityData() != null && preCheck.getHumidityData().contains("high")) {
                    riskFactors.add("金属文物面临高湿环境风险，可能加速锈蚀");
                    suggestions.add("需使用干燥剂控制湿度在45%以下");
                    requiresApproval = true;
                    showcaseSuggestion = "充氮密封展柜";
                    monitoringEquipment.add("湿度监测仪");
                    monitoringEquipment.add("氧气浓度监测仪");
                }
            }
            
            if (materialLower.contains("纸") || materialLower.contains("纸质") || materialLower.contains("书画")) {
                if (preCheck.getLightingLayout() != null && preCheck.getLightingLayout().contains("high_illuminance")) {
                    riskFactors.add("纸质文物/书画面临高照度风险，可能导致纸张脆化、字迹褪色");
                    suggestions.add("需降低照度至50lux以下，展期不超过30天");
                    requiresApproval = true;
                    showcaseSuggestion = "低反射紫外线过滤玻璃展柜";
                    durationSuggestion = 30;
                    monitoringEquipment.add("照度计");
                    monitoringEquipment.add("紫外线监测仪");
                }
            }
        }
        
        if (preCheck.getSecurityPatrolPlan() == null || preCheck.getSecurityPatrolPlan().isEmpty()) {
            riskFactors.add("缺少安防巡更计划");
            suggestions.add("需提交完整的安防巡更计划");
            requiresApproval = true;
        }
        
        if (riskFactors.isEmpty()) {
            risk.setRiskLevel("LOW");
            risk.setMitigationSuggestions("环境条件符合藏品展示要求，无需特殊 mitigation");
        } else if (riskFactors.size() <= 2) {
            risk.setRiskLevel("MEDIUM");
        } else {
            risk.setRiskLevel("HIGH");
        }
        
        risk.setRiskFactors(String.join(";", riskFactors));
        risk.setMitigationSuggestions(String.join(";", suggestions));
        risk.setRequiresApproval(requiresApproval);
        risk.setShowcaseSuggestion(showcaseSuggestion);
        risk.setExhibitionDurationSuggestion(durationSuggestion);
        risk.setMonitoringEquipment(String.join(";", monitoringEquipment));
        
        return environmentRiskRepository.save(risk);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentPreCheck> listPreChecksByLoan(Long loanId) {
        List<EnvironmentPreCheck> preChecks = environmentPreCheckRepository.findByLoanIdOrderBySubmittedAtDesc(loanId);
        preChecks.forEach(this::loadSubmitterName);
        preChecks.forEach(this::loadRiskAssessment);
        return preChecks;
    }

    @Transactional(readOnly = true)
    public EnvironmentPreCheck getLatestPreCheck(Long loanId) {
        EnvironmentPreCheck preCheck = environmentPreCheckRepository.findFirstByLoanIdOrderBySubmittedAtDesc(loanId)
                .orElse(null);
        if (preCheck != null) {
            loadSubmitterName(preCheck);
            loadRiskAssessment(preCheck);
        }
        return preCheck;
    }

    private void loadSubmitterName(EnvironmentPreCheck preCheck) {
        if (preCheck.getSubmittedBy() != null) {
            Optional<User> user = userRepository.findById(preCheck.getSubmittedBy());
            user.ifPresent(u -> preCheck.setSubmitterName(u.getName()));
        }
    }

    private void loadRiskAssessment(EnvironmentPreCheck preCheck) {
        if (preCheck.getId() != null) {
            Optional<EnvironmentRisk> risk = environmentRiskRepository.findByEnvironmentPrecheckId(preCheck.getId());
            risk.ifPresent(preCheck::setLatestRisk);
        }
    }

    @Transactional
    public EnvironmentRisk mitigateRisk(Long preCheckId, String mitigationActions) {
        EnvironmentPreCheck preCheck = environmentPreCheckRepository.findById(preCheckId)
                .orElseThrow(() -> new RuntimeException("环境预审记录不存在"));
        
        EnvironmentRisk risk = environmentRiskRepository.findByEnvironmentPrecheckId(preCheckId)
                .orElseThrow(() -> new RuntimeException("风险评估记录不存在"));
        
        String currentSuggestions = risk.getMitigationSuggestions();
        risk.setMitigationSuggestions(currentSuggestions + ";已执行 mitigation: " + mitigationActions);
        risk.setRequiresApproval(false);
        risk.setAssessedAt(LocalDateTime.now());
        
        return environmentRiskRepository.save(risk);
    }
}
