package com.relic.platform.repository;

import com.relic.platform.entity.Restoration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RestorationRepository extends JpaRepository<Restoration, Long> {

    @Query("SELECT r FROM Restoration r WHERE :status IS NULL OR r.status = :status")
    Page<Restoration> findByStatus(@Param("status") String status, Pageable pageable);
}
