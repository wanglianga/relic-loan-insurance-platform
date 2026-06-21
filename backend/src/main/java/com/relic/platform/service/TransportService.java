package com.relic.platform.service;

import com.relic.platform.entity.Transport;
import com.relic.platform.entity.TransportMonitor;
import com.relic.platform.entity.User;
import com.relic.platform.repository.TransportMonitorRepository;
import com.relic.platform.repository.TransportRepository;
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
public class TransportService {

    @Autowired
    private TransportRepository transportRepository;

    @Autowired
    private TransportMonitorRepository transportMonitorRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Transport> list(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sealedAt"));
        Page<Transport> transportPage = transportRepository.findByStatus(status, pageable);
        transportPage.forEach(this::loadBasicRelations);
        return transportPage;
    }

    @Transactional(readOnly = true)
    public Transport getById(Long id) {
        Transport transport = transportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("运输记录不存在"));
        loadBasicRelations(transport);
        loadMonitors(transport);
        return transport;
    }

    private void loadBasicRelations(Transport transport) {
        if (transport.getSealedBy() != null) {
            Optional<User> sealer = userRepository.findById(transport.getSealedBy());
            sealer.ifPresent(u -> transport.setSealerName(u.getName()));
        }
        if (transport.getEscortId() != null) {
            Optional<User> escort = userRepository.findById(transport.getEscortId());
            escort.ifPresent(u -> transport.setEscortName(u.getName()));
        }
    }

    private void loadMonitors(Transport transport) {
        List<TransportMonitor> monitors = transportMonitorRepository.findByTransportIdOrderByRecordedAtDesc(transport.getId());
        transport.setMonitors(monitors);
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
