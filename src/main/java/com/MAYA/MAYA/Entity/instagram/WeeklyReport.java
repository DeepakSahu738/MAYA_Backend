package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_reports",
       uniqueConstraints = @UniqueConstraint(columnNames = {"creator_id", "week_start_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "weekly_report_seq")
    @SequenceGenerator(name = "weekly_report_seq", sequenceName = "weekly_reports_id_seq", allocationSize = 50)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
    
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;
    
    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();
    
    @Column(name = "report_json", columnDefinition = "TEXT")
    private String reportJson;
    
    @Column(name = "health_score")
    private Double healthScore;
    
    @Column(name = "health_score_delta")
    private Double healthScoreDelta;
    
    @Column(name = "avg_engagement_rate")
    private Double avgEngagementRate;
    
    @Column(name = "avg_save_rate")
    private Double avgSaveRate;
    
    @Column(name = "avg_share_rate")
    private Double avgShareRate;
    
    @Column(name = "avg_reach_efficiency")
    private Double avgReachEfficiency;
    
    @Column(name = "posts_published")
    private Integer postsPublished = 0;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "top_post_id")
    private Post topPost;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worst_post_id")
    private Post worstPost;
    
    @Column(name = "sentiment_positive_pct")
    private Double sentimentPositivePct;
    
    @Column(name = "sentiment_neutral_pct")
    private Double sentimentNeutralPct;
    
    @Column(name = "sentiment_negative_pct")
    private Double sentimentNegativePct;
    
    @Column(name = "unanswered_questions_count")
    private Integer unansweredQuestionsCount = 0;
}
