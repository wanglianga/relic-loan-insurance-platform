package com.relic.platform.service;

import com.relic.platform.entity.Artifact;
import com.relic.platform.entity.EvidenceLog;
import com.relic.platform.entity.User;
import com.relic.platform.repository.ArtifactRepository;
import com.relic.platform.repository.EvidenceLogRepository;
import com.relic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EvidenceChainService {

    @Autowired
    private EvidenceLogRepository evidenceLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Transactional(readOnly = true)
    public Page<EvidenceLog> list(int page, int size, Long artifactId, String action, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EvidenceLog> logPage = evidenceLogRepository.findByConditions(artifactId, action, keyword, pageable);
        logPage.forEach(this::loadRelations);
        return logPage;
    }

    @Transactional(readOnly = true)
    public List<EvidenceLog> listByArtifact(Long artifactId) {
        List<EvidenceLog> logs = evidenceLogRepository.findByArtifactIdOrderByCreatedAtDesc(artifactId);
        logs.forEach(this::loadRelations);
        return logs;
    }

    private void loadRelations(EvidenceLog log) {
        if (log.getOperatorId() != null) {
            Optional<User> operator = userRepository.findById(log.getOperatorId());
            operator.ifPresent(u -> log.setOperatorName(u.getName()));
        }
        if (log.getArtifactId() != null) {
            Optional<Artifact> artifact = artifactRepository.findById(log.getArtifactId());
            artifact.ifPresent(a -> log.setArtifactName(a.getName()));
        }
    }

    @Transactional
    public EvidenceLog createLog(EvidenceLog log) {
        log.setId(null);
        log.setCreatedAt(LocalDateTime.now());
        return evidenceLogRepository.save(log);
    }
}
