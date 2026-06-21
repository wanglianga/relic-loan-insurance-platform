package com.relic.platform.repository;

import com.relic.platform.entity.RestorationStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestorationStepRepository extends JpaRepository<RestorationStep, Long> {

    List<RestorationStep> findByRestorationIdOrderByStepOrder(Long restorationId);
}
