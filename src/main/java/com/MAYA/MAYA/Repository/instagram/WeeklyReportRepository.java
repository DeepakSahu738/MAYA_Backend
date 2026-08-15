package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
