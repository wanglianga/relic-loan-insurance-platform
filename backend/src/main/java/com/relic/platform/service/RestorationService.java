package com.relic.platform.service;

import com.relic.platform.entity.Restoration;
import com.relic.platform.entity.RestorationStep;
import com.relic.platform.repository.ArtifactRepository;
import com.relic.platform.repository.RestorationRepository;
import com.relic.platform.repository.RestorationStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestorationService {

    @Autowired
    private RestorationRepository restorationRepository;

    @Autowired
    private RestorationStepRepository restorationStepRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Transactional(readOnly = true)
    public Page<Restoration> list(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "proposedAt"));
        return restorationRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Restoration getById(Long id) {
        return restorationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("修复记录不存在"));
    }

    @Transactional
    public Restoration create(Restoration restoration, Long proposerId) {
        if (!artifactRepository.existsById(restoration.getArtifactId())) {
            throw new RuntimeException("藏品不存在");
        }
        restoration.setId(null);
        restoration.setProposedBy(proposerId);
        restoration.setProposedAt(LocalDateTime.now());
        restoration.setStatus("PENDING");
        return restorationRepository.save(restoration);
    }

    @Transactional
    public Restoration approve(Long id, Long approverId) {
        Restoration restoration = getById(id);
        if (!"PENDING".equals(restoration.getStatus())) {
            throw new RuntimeException("修复记录状态不允许审批");
        }
        restoration.setStatus("APPROVED");
        restoration.setApprovedBy(approverId);
        restoration.setApprovedAt(LocalDateTime.now());
        return restorationRepository.save(restoration);
    }

    @Transactional
    public Restoration complete(Long id) {
        Restoration restoration = getById(id);
        if (!"APPROVED".equals(restoration.getStatus()) && !"IN_PROGRESS".equals(restoration.getStatus())) {
            throw new RuntimeException("修复记录状态不允许完成");
        }
        restoration.setStatus("COMPLETED");
        restoration.setCompletedAt(LocalDateTime.now());
        return restorationRepository.save(restoration);
    }

    @Transactional
    public RestorationStep addStep(Long restorationId, RestorationStep step, Long operatorId) {
        Restoration restoration = getById(restorationId);
        if (!"APPROVED".equals(restoration.getStatus()) && !"IN_PROGRESS".equals(restoration.getStatus())) {
            throw new RuntimeException("修复记录状态不允许添加步骤");
        }

        List<RestorationStep> steps = restorationStepRepository.findByRestorationIdOrderByStepOrder(restorationId);
        int nextOrder = steps.size() + 1;

        step.setId(null);
        step.setRestorationId(restorationId);
        step.setStepOrder(nextOrder);
        step.setOperatorId(operatorId);
        step.setOperatedAt(LocalDateTime.now());

        if ("APPROVED".equals(restoration.getStatus())) {
            restoration.setStatus("IN_PROGRESS");
            restorationRepository.save(restoration);
        }

        return restorationStepRepository.save(step);
    }
}
