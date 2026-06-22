package com.relic.platform.repository;

import com.relic.platform.entity.EnvironmentPreCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentPreCheckRepository extends JpaRepository<EnvironmentPreCheck, Long> {

    List<EnvironmentPreCheck> findByLoanIdOrderBySubmittedAtDesc(Long loanId);

    Optional<EnvironmentPreCheck> findFirstByLoanIdOrderBySubmittedAtDesc(Long loanId);
}
