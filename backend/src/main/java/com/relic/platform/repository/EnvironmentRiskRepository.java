package com.relic.platform.repository;

import com.relic.platform.entity.EnvironmentRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnvironmentRiskRepository extends JpaRepository<EnvironmentRisk, Long> {

    Optional<EnvironmentRisk> findByEnvironmentPrecheckId(Long environmentPrecheckId);
}
