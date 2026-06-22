package com.relic.platform.repository;

import com.relic.platform.entity.Disease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Long>, JpaSpecificationExecutor<Disease> {

    List<Disease> findByArtifactId(Long artifactId);

    List<Disease> findByReviewStatus(String reviewStatus);

    List<Disease> findByArtifactIdAndReviewStatus(Long artifactId, String reviewStatus);
}
