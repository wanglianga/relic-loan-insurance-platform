package com.relic.platform.repository;

import com.relic.platform.entity.Insurance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceRepository extends JpaRepository<Insurance, Long> {

    @Query("SELECT i FROM Insurance i WHERE :status IS NULL OR i.status = :status")
    Page<Insurance> findByStatus(@Param("status") String status, Pageable pageable);
}
