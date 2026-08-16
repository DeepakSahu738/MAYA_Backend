package com.MAYA.MAYA.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_goals",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "creator_id", "week_start"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "weekly_goal_seq")
    @SequenceGenerator(name = "weekly_goal_seq", sequenceName = "weekly_goals_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "target", nullable = false)
    private Integer target = 5;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
