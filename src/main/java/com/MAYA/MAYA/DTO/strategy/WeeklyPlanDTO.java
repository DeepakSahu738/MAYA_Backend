package com.MAYA.MAYA.DTO.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * The weekly content plan output — 7 days of content ideas.
 * This is the core "wow" response that makes MAYA a creator coach.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPlanDTO {

    private Long creatorId;
    private String username;
    private String weekStart; // ISO date string
    private String weekEnd;
    private List<String> detectedPillars; // content themes identified from recent posts
    private String bestPerformingStyle; // what type of content works best
    private List<DayPlanDTO> days; // 7 days of content
    private String strategyNotes; // overall strategy insight from LLM

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlanDTO {
        private String day; // MONDAY, TUESDAY, etc.
        private String date; // ISO date string
        private String contentPillar; // which pillar this post serves
        private String postIdea; // the idea/topic
        private String captionDraft; // AI-generated caption
        private String hook; // first line / attention grabber
        private String format; // IMAGE, VIDEO, REELS, CAROUSEL
        private String platform; // INSTAGRAM, YOUTUBE, TIKTOK, etc.
        private List<String> hashtags; // suggested hashtags
        private String bestTime; // e.g. "09:00"
        private String callToAction; // suggested CTA
        private String repurposeNote; // how to adapt for other platforms
    }
}
