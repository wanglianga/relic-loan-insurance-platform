package com.relic.platform.repository;

import com.relic.platform.entity.EvidenceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceLogRepository extends JpaRepository<EvidenceLog, Long> {

    List<EvidenceLog> findByArtifactIdOrderByCreatedAtDesc(Long artifactId);

    @Query("SELECT e FROM EvidenceLog e WHERE " +
           "(:artifactId IS NULL OR e.artifactId = :artifactId) AND " +
           "(:action IS NULL OR e.action = :action) AND " +
           "(:keyword IS NULL OR e.description LIKE %:keyword% OR e.responsibilityImpact LIKE %:keyword%)")
    Page<EvidenceLog> findByConditions(@Param("artifactId") Long artifactId,
                                       @Param("action") String action,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);
}
