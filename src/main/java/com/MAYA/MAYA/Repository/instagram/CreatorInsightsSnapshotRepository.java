package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.CreatorInsightsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreatorInsightsSnapshotRepository extends JpaRepository<CreatorInsightsSnapshot, Long> {
    List<CreatorInsightsSnapshot> findByCreatorIdOrderBySnapshotDateDesc(Long creatorId);
    List<CreatorInsightsSnapshot> findTop2ByCreatorIdOrderBySnapshotDateDesc(Long creatorId);
    Optional<CreatorInsightsSnapshot> findByCreatorIdAndSnapshotDate(Long creatorId, LocalDate date);
}
