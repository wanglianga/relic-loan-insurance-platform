package com.relic.platform.service;

import com.relic.platform.entity.Artifact;
import com.relic.platform.entity.ArtifactRestriction;
import com.relic.platform.repository.ArtifactRepository;
import com.relic.platform.repository.ArtifactRestrictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ArtifactService {

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private ArtifactRestrictionRepository artifactRestrictionRepository;

    @Transactional(readOnly = true)
    public Page<Artifact> list(int page, int size, String status, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return artifactRepository.findByConditions(status, keyword, pageable);
    }

    @Transactional(readOnly = true)
    public Artifact getById(Long id) {
        return artifactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("藏品不存在"));
    }

    @Transactional
    public Artifact create(Artifact artifact) {
        artifact.setId(null);
        artifact.setCreatedAt(LocalDateTime.now());
        artifact.setUpdatedAt(LocalDateTime.now());
        return artifactRepository.save(artifact);
    }

    @Transactional
    public Artifact update(Long id, Artifact artifact) {
        Artifact existing = getById(id);
        existing.setName(artifact.getName());
        existing.setEra(artifact.getEra());
        existing.setMaterial(artifact.getMaterial());
        existing.setDimensions(artifact.getDimensions());
        existing.setSource(artifact.getSource());
        existing.setAcquisitionDate(artifact.getAcquisitionDate());
        existing.setStatus(artifact.getStatus());
        existing.setDescription(artifact.getDescription());
        existing.setPhotoUrl(artifact.getPhotoUrl());
        existing.setUpdatedAt(LocalDateTime.now());
        return artifactRepository.save(existing);
    }

    @Transactional
    public ArtifactRestriction updateRestriction(Long artifactId, ArtifactRestriction restriction) {
        Artifact artifact = getById(artifactId);
        ArtifactRestriction existing = artifactRestrictionRepository.findByArtifactId(artifactId)
                .orElse(null);

        if (existing == null) {
            restriction.setId(null);
            restriction.setArtifactId(artifactId);
            return artifactRestrictionRepository.save(restriction);
        }

        existing.setNoExhibition(restriction.getNoExhibition());
        existing.setNoExhibitionReason(restriction.getNoExhibitionReason());
        existing.setNoLoan(restriction.getNoLoan());
        existing.setNoLoanReason(restriction.getNoLoanReason());
        existing.setMinLoanIntervalDays(restriction.getMinLoanIntervalDays());
        existing.setMaxLoanDays(restriction.getMaxLoanDays());
        existing.setLoanConditions(restriction.getLoanConditions());
        return artifactRestrictionRepository.save(existing);
    }
}
