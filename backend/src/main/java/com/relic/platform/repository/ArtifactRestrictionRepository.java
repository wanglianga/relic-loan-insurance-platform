package com.relic.platform.repository;

import com.relic.platform.entity.ArtifactRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtifactRestrictionRepository extends JpaRepository<ArtifactRestriction, Long> {

    Optional<ArtifactRestriction> findByArtifactId(Long artifactId);
}
