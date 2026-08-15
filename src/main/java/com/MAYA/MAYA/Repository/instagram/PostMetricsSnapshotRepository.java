package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.PostMetricsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostMetricsSnapshotRepository extends JpaRepository<PostMetricsSnapshot, Long> {
    List<PostMetricsSnapshot> findByPostIdOrderBySnapshotDateDesc(Long postId);
    Optional<PostMetricsSnapshot> findByPostIdAndSnapshotDate(Long postId, LocalDate date);
    List<PostMetricsSnapshot> findTop2ByPostIdOrderBySnapshotDateDesc(Long postId);
}
