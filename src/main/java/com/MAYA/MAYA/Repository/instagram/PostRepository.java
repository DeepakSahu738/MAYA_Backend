package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findByInstagramId(String instagramId);
    List<Post> findByCreatorIdOrderByPostedAtDesc(Long creatorId);
    List<Post> findByCreatorIdAndPostedAtBetween(Long creatorId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT p FROM Post p WHERE p.creator.id = :creatorId ORDER BY p.metrics.engagementRate DESC")
    List<Post> findTopPerformingPosts(Long creatorId);
    
    // New query methods
    List<Post> findByCreatorIdOrderByMetricsEngagementRateDesc(Long creatorId);
    List<Post> findByCreatorIdOrderByMetricsSaveRateDesc(Long creatorId);
    List<Post> findByCreatorIdAndMediaType(Long creatorId, String mediaType);
    List<Post> findByCreatorIdAndHasCtaTrue(Long creatorId);
    List<Post> findByCreatorIdAndHasQuestionTrue(Long creatorId);
    List<Post> findByCreatorIdAndMediaProductType(Long creatorId, String mediaProductType);
    void deleteByCreatorId(Long creatorId);

    @Query("SELECT p.instagramId FROM Post p WHERE p.creator.id = :creatorId")
    List<String> findInstagramIdsByCreatorId(Long creatorId);
}
