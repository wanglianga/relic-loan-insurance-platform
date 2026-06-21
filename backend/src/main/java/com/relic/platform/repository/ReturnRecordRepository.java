package com.relic.platform.repository;

import com.relic.platform.entity.ReturnRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {

    @Query("SELECT r FROM ReturnRecord r WHERE :status IS NULL OR r.overallStatus = :status")
    Page<ReturnRecord> findByStatus(@Param("status") String status, Pageable pageable);

    Optional<ReturnRecord> findByLoanId(Long loanId);
}
