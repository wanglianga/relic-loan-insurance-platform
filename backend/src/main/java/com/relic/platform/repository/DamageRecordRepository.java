package com.relic.platform.repository;

import com.relic.platform.entity.DamageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DamageRecordRepository extends JpaRepository<DamageRecord, Long> {

    List<DamageRecord> findByReturnId(Long returnId);
}
