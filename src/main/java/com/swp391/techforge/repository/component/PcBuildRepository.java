package com.swp391.techforge.repository.component;

import com.swp391.techforge.entity.PcBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcBuildRepository extends JpaRepository<PcBuild, Long> {
    List<PcBuild> findByUserUserId(Long userId);
}
