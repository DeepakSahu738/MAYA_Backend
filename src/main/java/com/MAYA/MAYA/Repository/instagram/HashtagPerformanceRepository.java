package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.HashtagPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagPerformanceRepository extends JpaRepository<HashtagPerformance, Long> {
    List<HashtagPerformance> findByCreatorIdOrderByPerformanceScoreDesc(Long creatorId);
    List<HashtagPerformance> findByCreatorIdOrderByUsageCountDesc(Long creatorId);
    Optional<HashtagPerformance> findByCreatorIdAndHashtag(Long creatorId, String hashtag);
    void deleteByCreatorId(Long creatorId);
}
