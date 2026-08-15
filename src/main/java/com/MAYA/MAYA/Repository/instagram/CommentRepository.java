package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByInstagramId(String instagramId);
    List<Comment> findByPostId(Long postId);
    List<Comment> findByPostCreatorId(Long creatorId);
    
    @Query("SELECT c FROM Comment c WHERE c.post.creator.id = :creatorId AND c.isQuestion = true")
    List<Comment> findQuestionsByCreatorId(Long creatorId);
    
    // Query methods using denormalized creator_id
    List<Comment> findByCreatorIdOrderByCommentedAtDesc(Long creatorId);
    List<Comment> findByCreatorIdAndSentiment(Long creatorId, String sentiment);
    
    // Existing query methods
    List<Comment> findByPostIdAndIsReplyFalseOrderByLikeCountDesc(Long postId);
    List<Comment> findByPostIdAndIsQuestionTrueAndSentimentNot(Long postId, String sentiment);
    List<Comment> findByPostIdOrderByLikeCountDesc(Long postId);
    void deleteByCreatorId(Long creatorId);
}
