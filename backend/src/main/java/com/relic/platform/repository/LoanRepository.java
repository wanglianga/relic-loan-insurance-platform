package com.relic.platform.repository;

import com.relic.platform.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query("SELECT l FROM Loan l WHERE " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:applicantId IS NULL OR l.applicantId = :applicantId)")
    Page<Loan> findByConditions(@Param("status") String status,
                                @Param("applicantId") Long applicantId,
                                Pageable pageable);
}
