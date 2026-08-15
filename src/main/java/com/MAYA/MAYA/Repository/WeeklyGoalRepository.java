package com.MAYA.MAYA.Repository;

import com.MAYA.MAYA.Entity.WeeklyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, Long> {
    Optional<WeeklyGoal> findByUserIdAndCreatorIdAndWeekStart(Long userId, Long creatorId, LocalDate weekStart);
}
