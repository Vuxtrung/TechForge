package com.swp391.techforge.repository;

import com.swp391.techforge.entity.CompatibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, Long> {
    List<CompatibilityRule> findByIsActiveTrue();
}
