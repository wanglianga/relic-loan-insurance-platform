package com.relic.platform.service;

import com.relic.platform.entity.Disease;
import com.relic.platform.entity.User;
import com.relic.platform.repository.DiseaseRepository;
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
public class DiseaseService {

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Disease> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"));
        Page<Disease> diseasePage = diseaseRepository.findAll(pageable);
        diseasePage.forEach(this::loadReporterName);
        return diseasePage;
    }

    @Transactional(readOnly = true)
    public List<Disease> listByArtifact(Long artifactId) {
        List<Disease> diseases = diseaseRepository.findByArtifactId(artifactId);
        diseases.forEach(this::loadReporterName);
        return diseases;
    }

    private void loadReporterName(Disease disease) {
        if (disease.getReportedBy() != null) {
            Optional<User> user = userRepository.findById(disease.getReportedBy());
            user.ifPresent(u -> disease.setReporterName(u.getName()));
        }
    }

    @Transactional
    public Disease create(Disease disease, Long reporterId) {
        disease.setId(null);
        disease.setReportedBy(reporterId);
        disease.setReportedAt(LocalDateTime.now());
        return diseaseRepository.save(disease);
    }
}
