package com.relic.platform.repository;

import com.relic.platform.entity.TransportMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportMonitorRepository extends JpaRepository<TransportMonitor, Long> {

    List<TransportMonitor> findByTransportIdOrderByRecordedAtDesc(Long transportId);
}
