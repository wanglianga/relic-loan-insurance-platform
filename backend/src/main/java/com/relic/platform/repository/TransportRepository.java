package com.relic.platform.repository;

import com.relic.platform.entity.Transport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransportRepository extends JpaRepository<Transport, Long> {

    @Query("SELECT t FROM Transport t WHERE :status IS NULL OR t.status = :status")
    Page<Transport> findByStatus(@Param("status") String status, Pageable pageable);
}
