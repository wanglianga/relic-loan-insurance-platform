package com.relic.platform.service;

import com.relic.platform.entity.Transport;
import com.relic.platform.entity.TransportMonitor;
import com.relic.platform.repository.TransportMonitorRepository;
import com.relic.platform.repository.TransportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransportService {

    @Autowired
    private TransportRepository transportRepository;

    @Autowired
    private TransportMonitorRepository transportMonitorRepository;

    @Transactional(readOnly = true)
    public Page<Transport> list(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sealedAt"));
        return transportRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Transport getById(Long id) {
        return transportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("运输记录不存在"));
    }

    @Transactional
    public Transport create(Transport transport) {
        transport.setId(null);
        transport.setStatus("DRAFT");
        return transportRepository.save(transport);
    }

    @Transactional
    public Transport seal(Long id, Transport transport, Long sealerId) {
        Transport existing = getById(id);
        if (!"DRAFT".equals(existing.getStatus())) {
            throw new RuntimeException("运输记录状态不允许封箱");
        }
        existing.setPackagingStandard(transport.getPackagingStandard());
        existing.setBoxCode(transport.getBoxCode());
        existing.setBoxSpec(transport.getBoxSpec());
        existing.setInnerMaterial(transport.getInnerMaterial());
        existing.setSealCode(transport.getSealCode());
        existing.setEscortId(transport.getEscortId());
        existing.setRoute(transport.getRoute());
        existing.setSealedBy(sealerId);
        existing.setSealedAt(LocalDateTime.now());
        existing.setStatus("SEALED");
        return transportRepository.save(existing);
    }

    @Transactional
    public Transport depart(Long id) {
        Transport transport = getById(id);
        if (!"SEALED".equals(transport.getStatus())) {
            throw new RuntimeException("运输记录状态不允许发车");
        }
        transport.setStatus("IN_TRANSIT");
        transport.setDepartedAt(LocalDateTime.now());
        return transportRepository.save(transport);
    }

    @Transactional
    public Transport arrive(Long id) {
        Transport transport = getById(id);
        if (!"IN_TRANSIT".equals(transport.getStatus())) {
            throw new RuntimeException("运输记录状态不允许到达");
        }
        transport.setStatus("ARRIVED");
        transport.setArrivedAt(LocalDateTime.now());
        return transportRepository.save(transport);
    }

    @Transactional
    public TransportMonitor addMonitor(Long transportId, TransportMonitor monitor) {
        Transport transport = getById(transportId);
        if (!"IN_TRANSIT".equals(transport.getStatus())) {
            throw new RuntimeException("运输记录状态不允许添加监控数据");
        }
        monitor.setId(null);
        monitor.setTransportId(transportId);
        monitor.setRecordedAt(LocalDateTime.now());
        return transportMonitorRepository.save(monitor);
    }
}
