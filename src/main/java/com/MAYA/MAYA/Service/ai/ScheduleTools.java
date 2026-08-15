package com.MAYA.MAYA.Service.ai;

import com.MAYA.MAYA.DTO.analytics.DayHourRecommendationDTO;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Entity.instagram.ScheduledPost;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.ScheduledPostRepository;
import com.MAYA.MAYA.Service.analytics.SnapshotAnalyticsService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Schedule @Tool methods — the LLM calls these to manage the content calendar.
 */
@Component
@RequiredArgsConstructor
public class ScheduleTools {

    private final ScheduledPostRepository scheduledPostRepository;
    private final CreatorRepository creatorRepository;
    private final SnapshotAnalyticsService snapshotAnalyticsService;

    @Tool("Suggest the best time slots to post this week based on historical engagement data")
    public String suggestSlots(@P("The creator's database ID") Long creatorId) {
        DayHourRecommendationDTO best = snapshotAnalyticsService.getBestDayHourToPost(creatorId);

        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

        StringBuilder sb = new StringBuilder("Recommended posting slots this week:\n");
        sb.append(String.format("1. %s at %d:00 (best day + hour based on your data)\n", best.getBestDay(), best.getBestHour()));

        // Suggest 2 more slots spread across the week
        DayOfWeek bestDay = DayOfWeek.valueOf(best.getBestDay());
        DayOfWeek altDay1 = bestDay.plus(2);
        DayOfWeek altDay2 = bestDay.plus(4);
        sb.append(String.format("2. %s at %d:00\n", altDay1.name(), best.getBestHour()));
        sb.append(String.format("3. %s at %d:00\n", altDay2.name(), best.getBestHour()));
        sb.append("\nThese are based on when your audience historically engages most.");

        return sb.toString();
    }

    @Tool("Create a draft post on the content calendar with a caption and scheduled time")
    public String createDraft(
            @P("The creator's database ID") Long creatorId,
            @P("The post caption text") String caption,
            @P("Media type: IMAGE or VIDEO") String mediaType,
            @P("Comma-separated hashtags") String hashtags,
            @P("Scheduled date and time in format yyyy-MM-ddTHH:mm") String scheduledFor) {

        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        if (creator == null) return "Creator not found.";

        LocalDateTime scheduleTime;
        try {
            scheduleTime = LocalDateTime.parse(scheduledFor);
        } catch (Exception e) {
            return "Invalid date format. Use yyyy-MM-ddTHH:mm (e.g., 2026-07-10T09:00)";
        }

        ScheduledPost post = new ScheduledPost();
        post.setCreator(creator);
        post.setCaption(caption);
        post.setMediaType(mediaType != null ? mediaType : "IMAGE");
        post.setHashtags(hashtags);
        post.setScheduledFor(scheduleTime);
        post.setApprovalStatus(ScheduledPost.ApprovalStatus.PENDING);

        post = scheduledPostRepository.save(post);

        return String.format("Draft created! ID: %d\nCaption: \"%s\"\nScheduled: %s\nStatus: PENDING (needs your approval)",
            post.getId(), truncate(caption, 60), scheduleTime);
    }

    @Tool("List all scheduled/draft posts on the content calendar for a creator")
    public String listScheduledPosts(@P("The creator's database ID") Long creatorId) {
        List<ScheduledPost> posts = scheduledPostRepository.findByCreatorIdOrderByScheduledForDesc(creatorId);
        if (posts.isEmpty()) return "No scheduled posts found. Want me to suggest some time slots?";

        return posts.stream()
            .limit(10)
            .map(p -> String.format("- [ID:%d] [%s] \"%s\" → %s (%s)",
                p.getId(), p.getApprovalStatus().name(),
                truncate(p.getCaption(), 40),
                p.getScheduledFor().toString(),
                p.getMediaType()))
            .collect(Collectors.joining("\n"));
    }

    @Tool("Update a scheduled post's caption, hashtags, media type, or time")
    public String updateDraft(
            @P("The scheduled post ID to update") Long postId,
            @P("New caption (or null to keep current)") String caption,
            @P("New hashtags comma-separated (or null to keep current)") String hashtags,
            @P("New scheduled date and time in format yyyy-MM-ddTHH:mm (or null to keep current)") String scheduledFor) {

        ScheduledPost post = scheduledPostRepository.findById(postId).orElse(null);
        if (post == null) return "Post not found with ID: " + postId;

        if (caption != null && !caption.isBlank()) post.setCaption(caption);
        if (hashtags != null && !hashtags.isBlank()) post.setHashtags(hashtags);
        if (scheduledFor != null && !scheduledFor.isBlank()) {
            try {
                post.setScheduledFor(java.time.LocalDateTime.parse(scheduledFor));
            } catch (Exception e) {
                return "Invalid date format. Use yyyy-MM-ddTHH:mm (e.g., 2026-07-15T09:00)";
            }
        }
        post.setUpdatedAt(java.time.LocalDateTime.now());
        scheduledPostRepository.save(post);

        return String.format("Updated post ID %d:\nCaption: \"%s\"\nScheduled: %s\nStatus: %s",
            post.getId(), truncate(post.getCaption(), 60), post.getScheduledFor(), post.getApprovalStatus());
    }

    @Tool("Delete a scheduled post from the content calendar")
    public String deleteDraft(@P("The scheduled post ID to delete") Long postId) {
        ScheduledPost post = scheduledPostRepository.findById(postId).orElse(null);
        if (post == null) return "Post not found with ID: " + postId;

        String caption = post.getCaption();
        scheduledPostRepository.deleteById(postId);

        return String.format("Deleted post ID %d: \"%s\"", postId, truncate(caption, 40));
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
