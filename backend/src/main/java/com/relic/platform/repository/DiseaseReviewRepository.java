package com.relic.platform.repository;

import com.relic.platform.entity.DiseaseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiseaseReviewRepository extends JpaRepository<DiseaseReview, Long> {

    List<DiseaseReview> findByDiseaseIdOrderByReviewedAtDesc(Long diseaseId);

    List<DiseaseReview> findByLoanId(Long loanId);

    Optional<DiseaseReview> findFirstByDiseaseIdOrderByReviewedAtDesc(Long diseaseId);
}
