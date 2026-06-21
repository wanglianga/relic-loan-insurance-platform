package com.relic.platform.service;

import com.relic.platform.entity.EnvironmentMonitor;
import com.relic.platform.entity.Exhibition;
import com.relic.platform.repository.EnvironmentMonitorRepository;
import com.relic.platform.repository.ExhibitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExhibitionService {

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @Autowired
    private EnvironmentMonitorRepository environmentMonitorRepository;

    @Transactional(readOnly = true)
    public Page<Exhibition> list(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "setupAt"));
        return exhibitionRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Exhibition getById(Long id) {
        return exhibitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("展览记录不存在"));
    }

    @Transactional
    public Exhibition setup(Exhibition exhibition) {
        exhibition.setId(null);
        exhibition.setStatus("SETUP");
        exhibition.setSetupAt(LocalDateTime.now());
        return exhibitionRepository.save(exhibition);
    }

    @Transactional
    public Exhibition confirmSetup(Long id, Long confirmerId) {
        Exhibition exhibition = getById(id);
        if (!"SETUP".equals(exhibition.getStatus())) {
            throw new RuntimeException("展览记录状态不允许确认布展");
        }
        exhibition.setStatus("ON_DISPLAY");
        exhibition.setSetupConfirmedBy(confirmerId);
        return exhibitionRepository.save(exhibition);
    }

    @Transactional
    public EnvironmentMonitor addEnvironmentMonitor(Long exhibitionId, EnvironmentMonitor monitor) {
        Exhibition exhibition = getById(exhibitionId);
        if (!"ON_DISPLAY".equals(exhibition.getStatus())) {
            throw new RuntimeException("展览记录状态不允许添加环境监控数据");
        }
        monitor.setId(null);
        monitor.setExhibitionId(exhibitionId);
        monitor.setRecordedAt(LocalDateTime.now());
        if (monitor.getIsAlert() == null) {
            monitor.setIsAlert(false);
        }
        return environmentMonitorRepository.save(monitor);
    }
}
