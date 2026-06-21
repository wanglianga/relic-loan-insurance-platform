package com.relic.platform.service;

import com.relic.platform.entity.EvidenceLog;
import com.relic.platform.repository.EvidenceLogRepository;
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
public class EvidenceChainService {

    @Autowired
    private EvidenceLogRepository evidenceLogRepository;

    @Transactional(readOnly = true)
    public Page<EvidenceLog> list(int page, int size, Long artifactId, String action, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return evidenceLogRepository.findByConditions(artifactId, action, keyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<EvidenceLog> listByArtifact(Long artifactId) {
        return evidenceLogRepository.findByArtifactIdOrderByCreatedAtDesc(artifactId);
    }

    @Transactional
    public EvidenceLog createLog(EvidenceLog log) {
        log.setId(null);
        log.setCreatedAt(LocalDateTime.now());
        return evidenceLogRepository.save(log);
    }
}
