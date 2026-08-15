package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.ScheduledPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, Long> {
    List<ScheduledPost> findByCreatorIdOrderByScheduledForDesc(Long creatorId);
    List<ScheduledPost> findByApprovalStatus(ScheduledPost.ApprovalStatus status);
    List<ScheduledPost> findByApprovalStatusAndScheduledForBefore(ScheduledPost.ApprovalStatus status, LocalDateTime time);
    void deleteByCreatorId(Long creatorId);
}
