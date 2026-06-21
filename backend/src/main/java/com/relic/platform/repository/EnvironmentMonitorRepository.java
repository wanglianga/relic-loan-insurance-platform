package com.relic.platform.repository;

import com.relic.platform.entity.EnvironmentMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvironmentMonitorRepository extends JpaRepository<EnvironmentMonitor, Long> {

    List<EnvironmentMonitor> findByExhibitionIdOrderByRecordedAtDesc(Long exhibitionId);
}
