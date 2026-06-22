package com.relic.platform.repository;

import com.relic.platform.entity.ExhibitionCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExhibitionConditionRepository extends JpaRepository<ExhibitionCondition, Long> {

    List<ExhibitionCondition> findByLoanId(Long loanId);

    Optional<ExhibitionCondition> findByDiseaseReviewId(Long diseaseReviewId);
}
