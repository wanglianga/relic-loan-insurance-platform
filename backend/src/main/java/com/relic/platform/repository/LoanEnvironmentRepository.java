package com.relic.platform.repository;

import com.relic.platform.entity.LoanEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanEnvironmentRepository extends JpaRepository<LoanEnvironment, Long> {

    Optional<LoanEnvironment> findByLoanId(Long loanId);
}
