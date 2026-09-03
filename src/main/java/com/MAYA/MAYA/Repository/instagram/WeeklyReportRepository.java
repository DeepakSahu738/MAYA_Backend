package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    List<WeeklyReport> findByCreatorIdOrderByWeekStartDateDesc(Long creatorId);
    Optional<WeeklyReport> findTopByCreatorIdOrderByWeekStartDateDesc(Long creatorId);
    Optional<WeeklyReport> findByCreatorIdAndWeekStartDate(Long creatorId, LocalDate weekStartDate);
    void deleteByCreatorId(Long creatorId);

    /**
     * Clears the top_post_id / worst_post_id FK references for a creator's weekly
     * reports. Must be called before deleting a creator's posts (e.g. in the
     * nightly re-sync), otherwise the post delete violates the FK constraint.
     * Historical reports are preserved — only the (soon-to-be-stale) post links
     * are nulled.
     */
    @Modifying
    @Query("UPDATE WeeklyReport w SET w.topPost = null, w.worstPost = null WHERE w.creator.id = :creatorId")
    void clearPostReferencesByCreatorId(@Param("creatorId") Long creatorId);
}
