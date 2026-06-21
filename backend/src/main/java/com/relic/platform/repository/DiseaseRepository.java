package com.relic.platform.repository;

import com.relic.platform.entity.Disease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Long> {

    List<Disease> findByArtifactId(Long artifactId);
}
