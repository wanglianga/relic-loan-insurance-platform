package com.relic.platform.repository;

import com.relic.platform.entity.Artifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    @Query("SELECT a FROM Artifact a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:keyword IS NULL OR a.name LIKE %:keyword% OR a.artifactCode LIKE %:keyword%)")
    Page<Artifact> findByConditions(@Param("status") String status,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    long countByStatus(String status);
}
