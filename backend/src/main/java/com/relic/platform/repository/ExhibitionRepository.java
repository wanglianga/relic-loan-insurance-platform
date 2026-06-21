package com.relic.platform.repository;

import com.relic.platform.entity.Exhibition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {

    @Query("SELECT e FROM Exhibition e WHERE :status IS NULL OR e.status = :status")
    Page<Exhibition> findByStatus(@Param("status") String status, Pageable pageable);
}
