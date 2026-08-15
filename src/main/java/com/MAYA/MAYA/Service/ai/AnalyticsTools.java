package com.MAYA.MAYA.Service.ai;

import com.MAYA.MAYA.DTO.analytics.*;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Service.analytics.AccountHealthService;
import com.MAYA.MAYA.Service.analytics.AnalyticsService;
import com.MAYA.MAYA.Service.analytics.SnapshotAnalyticsService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Analytics @Tool methods — the LLM calls these to answer data questions.
 * 
 * Rules:
 * - Pure data retrieval + formatting — no LLM calls here
 * - Each method returns a String (human-readable summary for the LLM to use)
 * - The creatorId parameter is injected by the LLM from the system prompt context
 */
@Component
@RequiredArgsConstructor
public class AnalyticsTools {

    private final AnalyticsService analyticsService;
    private final SnapshotAnalyticsService snapshotAnalyticsService;
    private final AccountHealthService accountHealthService;
    private final CreatorRepository creatorRepository;

    @Tool("Get the overall account health score (0-100) with component breakdown and improvement suggestions")
    public String getHealthScore(@P("The creator's database ID") Long creatorId) {
        AccountHealthScoreDTO dto = accountHealthService.calculateAccountHealthScore(creatorId);
        if (dto.getScore() == null) return "Insufficient data to calculate health score.";

        return String.format("Health Score: %d/100 (%s)\nComponents: %s\nStrengths: %s\nImprovements: %s",
            dto.getScore(), dto.getGrade(),
            dto.getComponentScores().toString(),
            String.join(", ", dto.getStrengths()),
            String.join(", ", dto.getImprovements()));
    }

    @Tool("Get the current engagement rate, save rate, share rate, and comment rate")
    public String getEngagementMetrics(@P("The creator's database ID") Long creatorId) {
        TimeSeriesMetricDTO er = analyticsService.calculateOverallEngagement(creatorId, null);
        TimeSeriesMetricDTO save = analyticsService.calculateSaveRate(creatorId, null);
        TimeSeriesMetricDTO share = analyticsService.calculateShareRate(creatorId, null);
        TimeSeriesMetricDTO comment = analyticsService.calculateCommentRate(creatorId, null);

        return String.format(
            "Engagement Rate: %s%%\nSave Rate: %s%%\nShare Rate: %s%%\nComment Rate: %s%%",
            format(er.getCurrentValue()), format(save.getCurrentValue()),
            format(share.getCurrentValue()), format(comment.getCurrentValue()));
    }

    @Tool("Get the top 5 best performing posts by engagement rate")
    public String getBestPosts(@P("The creator's database ID") Long creatorId) {
        List<PostPerformanceDTO> posts = snapshotAnalyticsService.getBestPerformingPosts(creatorId, 5);
        if (posts.isEmpty()) return "No posts with reach data available.";

        return posts.stream()
            .map(p -> String.format("- \"%s\" (%s) — ER: %.1f%%, Likes: %d, Saves: %s",
                truncate(p.getCaption(), 60), p.getMediaType(),
                p.getEngagementRate(), p.getLikes(),
                p.getSaves() != null ? p.getSaves().toString() : "N/A"))
            .collect(Collectors.joining("\n"));
    }

    @Tool("Get the top 5 worst performing posts by engagement rate")
    public String getWorstPosts(@P("The creator's database ID") Long creatorId) {
        List<PostPerformanceDTO> posts = snapshotAnalyticsService.getWorstPerformingPosts(creatorId, 5);
        if (posts.isEmpty()) return "No posts with reach data available.";

        return posts.stream()
            .map(p -> String.format("- \"%s\" (%s) — ER: %.1f%%, Likes: %d",
                truncate(p.getCaption(), 60), p.getMediaType(),
                p.getEngagementRate(), p.getLikes()))
            .collect(Collectors.joining("\n"));
    }

    @Tool("Get the top 10 best performing hashtags with reach and engagement data")
    public String getTopHashtags(@P("The creator's database ID") Long creatorId) {
        List<HashtagPerformanceDTO> tags = snapshotAnalyticsService.getTopPerformingHashtags(creatorId, 10);
        if (tags.isEmpty()) return "Not enough hashtag data (need at least 3 posts per hashtag).";

        return tags.stream()
            .map(t -> String.format("- %s (used %dx) — Avg ER: %s%%, Avg Reach: %s",
                t.getHashtag(), t.getUsageCount(),
                format(t.getAvgEngagementRate()),
                t.getAvgReach() != null ? String.format("%.0f", t.getAvgReach()) : "N/A"))
            .collect(Collectors.joining("\n"));
    }

    @Tool("Get the best day and hour to post based on historical engagement data")
    public String getBestPostingTime(@P("The creator's database ID") Long creatorId) {
        DayHourRecommendationDTO dto = snapshotAnalyticsService.getBestDayHourToPost(creatorId);
        return String.format("Best day: %s\nBest hour: %d:00\nAvg engagement rate at best time: %s%%\nTimezone: %s",
            dto.getBestDay(), dto.getBestHour(), format(dto.getAvgEngagementRate()), dto.getTimezone());
    }

    @Tool("Get the top 5 superfans/most active commenters")
    public String getTopCommenters(@P("The creator's database ID") Long creatorId) {
        List<TopCommenterDTO> fans = snapshotAnalyticsService.getTopCommenters(creatorId, 5);
        if (fans.isEmpty()) return "No comment data available.";

        return fans.stream()
            .map(f -> String.format("- @%s — %d comments, superfan score: %.0f",
                f.getUsername(), f.getCommentCount(), f.getAvgSentimentScore()))
            .collect(Collectors.joining("\n"));
    }

    @Tool("Get the sentiment breakdown of comments (positive, neutral, negative percentages)")
    public String getSentimentBreakdown(@P("The creator's database ID") Long creatorId) {
        SentimentBreakdownDTO dto = analyticsService.getSentimentBreakdown(creatorId);
        return String.format("Positive: %.1f%%\nNeutral: %.1f%%\nNegative: %.1f%%\nTotal comments analyzed: %d",
            dto.getPositivePercentage(), dto.getNeutralPercentage(), dto.getNegativePercentage(),
            dto.getPositiveCount() + dto.getNeutralCount() + dto.getNegativeCount());
    }

    @Tool("Get the most common words and topics in comments to understand audience interests")
    public String getCommonTopics(@P("The creator's database ID") Long creatorId) {
        List<CommentInsightDTO> words = snapshotAnalyticsService.getCommonCommentWords(creatorId, 15);
        if (words.isEmpty()) return "No comment data available.";

        return "Top words in comments:\n" + words.stream()
            .map(w -> String.format("- \"%s\" (mentioned %d times)", w.getWord(), w.getFrequency()))
            .collect(Collectors.joining("\n"));
    }

    @Tool("Get the posting frequency and content mix breakdown (image vs video performance)")
    public String getContentStrategy(@P("The creator's database ID") Long creatorId) {
        TimeSeriesMetricDTO freq = analyticsService.calculatePostingFrequency(creatorId);
        TimeSeriesMetricDTO mix = analyticsService.calculateContentMix(creatorId);
        TimeSeriesMetricDTO playthrough = analyticsService.calculatePlayThroughRate(creatorId, null);

        return String.format(
            "Posting frequency: %s posts/week\nContent mix video %%: %s%%\nPlay-through rate (videos): %s%%",
            format(freq.getCurrentValue()), format(mix.getCurrentValue()),
            format(playthrough.getCurrentValue()));
    }

    @Tool("Get the creator's profile information including username, niche, and follower count")
    public String getCreatorProfile(@P("The creator's database ID") Long creatorId) {
        Creator c = creatorRepository.findById(creatorId).orElse(null);
        if (c == null) return "Creator not found.";

        return String.format("Username: @%s\nNiche: %s\nFollowers: %s\nBio: %s",
            c.getUsername(), c.getNiche(),
            c.getFollowerCount() != null ? c.getFollowerCount().toString() : "N/A",
            c.getBiography() != null ? c.getBiography() : "Not set");
    }

    // --- Helpers ---

    private String format(Double value) {
        return value != null ? String.format("%.2f", value) : "N/A";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
