package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.TopCommenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopCommenterRepository extends JpaRepository<TopCommenter, Long> {
    List<TopCommenter> findByCreatorIdOrderBySuperfanScoreDesc(Long creatorId);
    List<TopCommenter> findByCreatorIdOrderByCommentCountDesc(Long creatorId);
    Optional<TopCommenter> findByCreatorIdAndUsername(Long creatorId, String username);
    void deleteByCreatorId(Long creatorId);
}
