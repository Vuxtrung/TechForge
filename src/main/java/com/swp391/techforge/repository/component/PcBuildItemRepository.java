package com.swp391.techforge.repository.component;

import com.swp391.techforge.entity.PcBuildItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcBuildItemRepository extends JpaRepository<PcBuildItem, Long> {
    List<PcBuildItem> findByPcBuildBuildId(Long buildId);
}
