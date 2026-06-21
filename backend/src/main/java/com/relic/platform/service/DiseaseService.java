package com.relic.platform.service;

import com.relic.platform.entity.Disease;
import com.relic.platform.repository.DiseaseRepository;
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
public class DiseaseService {

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Transactional(readOnly = true)
    public Page<Disease> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"));
        return diseaseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Disease> listByArtifact(Long artifactId) {
        return diseaseRepository.findByArtifactId(artifactId);
    }

    @Transactional
    public Disease create(Disease disease, Long reporterId) {
        disease.setId(null);
        disease.setReportedBy(reporterId);
        disease.setReportedAt(LocalDateTime.now());
        return diseaseRepository.save(disease);
    }
}
