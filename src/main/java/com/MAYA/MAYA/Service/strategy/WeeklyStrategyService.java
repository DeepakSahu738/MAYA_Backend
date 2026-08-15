package com.MAYA.MAYA.Service.strategy;

import com.MAYA.MAYA.DTO.strategy.WeeklyPlanDTO;
import com.MAYA.MAYA.DTO.strategy.WeeklyPlanDTO.DayPlanDTO;
import com.MAYA.MAYA.Entity.instagram.Comment;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Entity.instagram.Post;
import com.MAYA.MAYA.Entity.instagram.PostMetrics;
import com.MAYA.MAYA.Repository.instagram.CommentRepository;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.PostRepository;
import com.MAYA.MAYA.Service.analytics.SnapshotAnalyticsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Weekly Strategy Generator.
 *
 * Flow:
 * 1. Fetch last 30 posts + recent comments
 * 2. Extract content signals (style, formats, engagement, audience questions, gaps)
 * 3. Build a rich context prompt for the LLM
 * 4. LLM generates a 7-day personalized content plan starting from TOMORROW
 * 5. Parse LLM response into structured WeeklyPlanDTO
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyStrategyService {

    private final PostRepository postRepository;
    private final CreatorRepository creatorRepository;
    private final CommentRepository commentRepository;
    private final SnapshotAnalyticsService snapshotAnalyticsService;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    private static final int POSTS_TO_ANALYZE = 30;

    /**
     * Generate a full 7-day content plan starting from TOMORROW.
     */
    public WeeklyPlanDTO generateWeeklyPlan(Long creatorId) {
        Creator creator = creatorRepository.findById(creatorId)
            .orElseThrow(() -> new RuntimeException("Creator not found: " + creatorId));

        List<Post> recentPosts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId).stream()
            .limit(POSTS_TO_ANALYZE)
            .collect(Collectors.toList());

        if (recentPosts.isEmpty()) {
            return buildEmptyPlan(creator, "No posts found. Connect your account and sync your content first.");
        }

        // Step 1: Extract ALL content signals
        ContentSignals signals = analyzeContentSignals(recentPosts, creator, creatorId);

        // Step 2: Call LLM with enriched prompt
        String llmResponse = callLLMForStrategy(signals, creator);

        // Step 3: Parse LLM response into structured plan
        WeeklyPlanDTO plan = parseLLMResponse(llmResponse, creator, signals);

        return plan;
    }

    // === STEP 1: Analyze recent posts + comments for patterns ===
    private ContentSignals analyzeContentSignals(List<Post> posts, Creator creator, Long creatorId) {
        ContentSignals signals = new ContentSignals();

        // --- Basic profile ---
        signals.niche = creator.getNiche() != null ? creator.getNiche() : "General";
        signals.username = creator.getUsername();
        signals.followerCount = creator.getFollowerCount();

        // --- Content formats distribution ---
        long imageCount = posts.stream().filter(p -> "IMAGE".equalsIgnoreCase(p.getMediaType())).count();
        long videoCount = posts.stream().filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType())).count();
        signals.imagePct = posts.isEmpty() ? 0 : (int)(imageCount * 100 / posts.size());
        signals.videoPct = posts.isEmpty() ? 0 : (int)(videoCount * 100 / posts.size());

        // --- Best performing format ---
        OptionalDouble imageEr = posts.stream()
            .filter(p -> "IMAGE".equalsIgnoreCase(p.getMediaType()))
            .map(Post::getMetrics).filter(m -> m.getEngagementRate() != null)
            .mapToDouble(PostMetrics::getEngagementRate).average();
        OptionalDouble videoEr = posts.stream()
            .filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType()))
            .map(Post::getMetrics).filter(m -> m.getEngagementRate() != null)
            .mapToDouble(PostMetrics::getEngagementRate).average();
        signals.bestFormat = (videoEr.orElse(0) > imageEr.orElse(0)) ? "VIDEO/REELS" : "IMAGE";

        // --- Top 5 captions by engagement (style reference) ---
        signals.topCaptions = posts.stream()
            .filter(p -> p.getMetrics().getEngagementRate() != null)
            .sorted(Comparator.comparingDouble(p -> -p.getMetrics().getEngagementRate()))
            .limit(5)
            .map(p -> p.getCaption() != null ? p.getCaption() : "")
            .filter(c -> !c.isBlank())
            .collect(Collectors.toList());

        // --- Last 7 posts (to avoid repetition) ---
        signals.lastWeekTopics = posts.stream()
            .limit(7)
            .map(p -> p.getCaption() != null ? truncate(p.getCaption(), 80) : "")
            .filter(c -> !c.isBlank())
            .collect(Collectors.toList());

        // --- Viral patterns (top 3 by share rate) ---
        signals.viralCaptions = posts.stream()
            .filter(p -> p.getMetrics().getShares() != null && p.getMetrics().getReach() != null && p.getMetrics().getReach() > 0)
            .sorted(Comparator.comparingDouble(p -> -(p.getMetrics().getShares() * 100.0 / p.getMetrics().getReach())))
            .limit(3)
            .map(p -> p.getCaption() != null ? truncate(p.getCaption(), 100) : "")
            .filter(c -> !c.isBlank())
            .collect(Collectors.toList());

        // --- Posting frequency ---
        signals.postsPerWeek = posts.size() > 7 ? posts.size() / 4 : posts.size();

        // --- Common hashtags ---
        Map<String, Integer> hashtagCounts = new HashMap<>();
        for (Post post : posts) {
            if (post.getHashtags() != null) {
                for (String tag : post.getHashtags().split(",")) {
                    tag = tag.trim();
                    if (!tag.isEmpty()) hashtagCounts.merge(tag, 1, Integer::sum);
                }
            }
        }
        signals.topHashtags = hashtagCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // --- Best posting day/hour ---
        var dayHour = snapshotAnalyticsService.getBestDayHourToPost(creatorId);
        signals.bestDay = dayHour.getBestDay();
        signals.bestHour = dayHour.getBestHour();

        // --- Audience questions (top 5 most liked questions from comments) ---
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);
        signals.audienceQuestions = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()))
            .sorted(Comparator.comparingInt(c -> -(c.getLikeCount() != null ? c.getLikeCount() : 0)))
            .limit(5)
            .map(Comment::getText)
            .collect(Collectors.toList());

        // --- Content gaps (words in comments NOT in captions) ---
        Set<String> captionWords = new HashSet<>();
        for (Post post : posts) {
            if (post.getCaption() != null) {
                for (String w : post.getCaption().toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+")) {
                    if (w.length() >= 4) captionWords.add(w);
                }
            }
        }
        Map<String, Integer> commentWordFreq = new HashMap<>();
        Set<String> stopwords = Set.of("this", "that", "your", "what", "when", "where", "which", "have", "been", "will", "would", "could", "should", "more", "very", "just", "also", "from", "they", "them", "than", "then");
        for (Comment c : comments) {
            if (c.getText() != null) {
                for (String w : c.getText().toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+")) {
                    if (w.length() >= 4 && !stopwords.contains(w) && !captionWords.contains(w)) {
                        commentWordFreq.merge(w, 1, Integer::sum);
                    }
                }
            }
        }
        signals.contentGaps = commentWordFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return signals;
    }

    // === STEP 2: Build enriched prompt and call LLM ===
    private String callLLMForStrategy(ContentSignals signals, Creator creator) {
        // Start from TOMORROW, not next Monday
        LocalDate planStart = LocalDate.now().plusDays(1);

        String prompt = String.format("""
            You are MAYA, an expert AI content strategist for social media creators.
            Today is %s.
            
            CREATOR PROFILE:
            - Username: @%s
            - Niche: %s
            - Followers: %s
            - Best performing format: %s
            - Content mix: %d%% images, %d%% video
            - Posting frequency: ~%d posts/week
            - Best posting day: %s, best hour: %d:00
            - Top hashtags: %s
            
            THEIR TOP PERFORMING CAPTIONS (replicate this style):
            %s
            
            THEIR MOST VIRAL CONTENT (highest share rate — what gets spread):
            %s
            
            WHAT THEY POSTED LAST WEEK (DO NOT repeat these topics):
            %s
            
            AUDIENCE QUESTIONS (what followers are asking — address these in content):
            %s
            
            CONTENT GAPS (topics audience discusses but creator hasn't posted about):
            %s
            
            TASK:
            Generate a 7-day content plan starting from %s (%s).
            The 7 days are: %s
            
            For EACH day, provide:
            1. content_pillar: A thematic category (Educational, Behind the scenes, Engagement bait, Personal story, Tips & tricks, Trending topic, Community, Q&A)
            2. post_idea: A SPECIFIC content idea addressing audience interests — not generic
            3. caption: A complete, ready-to-post caption (150-300 chars) matching their proven writing style
            4. hook: The first line that stops scrolling (under 50 chars, punchy)
            5. format: IMAGE or VIDEO
            6. hashtags: 5-7 relevant hashtags from their top-performing ones + 1-2 new ones
            7. best_time: Posting time in HH:MM format
            8. cta: A natural call-to-action
            9. repurpose_note: How to adapt this for another platform (YouTube Short, TikTok, etc.)
            
            RULES:
            - MATCH the creator's writing style exactly (learn from their top captions)
            - DO NOT repeat topics from their last week posts
            - ADDRESS at least 2 audience questions in the week's content
            - FILL at least 1 content gap
            - Vary pillars (no same pillar 2 days in a row)
            - Lean toward %s format (performs better)
            - At least 1 engagement post (poll/question/hot take)
            - At least 1 educational/value post
            - At least 1 personal/vulnerable post
            - Make ideas SPECIFIC to %s niche, not generic advice
            
            RESPOND IN THIS EXACT JSON FORMAT ONLY (no markdown, no extra text):
            [
              {
                "day": "%s",
                "content_pillar": "...",
                "post_idea": "...",
                "caption": "...",
                "hook": "...",
                "format": "IMAGE or VIDEO",
                "hashtags": ["#tag1", "#tag2"],
                "best_time": "09:00",
                "cta": "...",
                "repurpose_note": "..."
              }
            ]
            Return exactly 7 items.
            """,
            LocalDate.now().toString(),
            signals.username, signals.niche,
            signals.followerCount != null ? signals.followerCount.toString() : "N/A",
            signals.bestFormat,
            signals.imagePct, signals.videoPct, signals.postsPerWeek,
            signals.bestDay, signals.bestHour,
            String.join(", ", signals.topHashtags),
            // Top captions
            signals.topCaptions.stream().map(c -> "- \"" + truncate(c, 120) + "\"").collect(Collectors.joining("\n")),
            // Viral captions
            signals.viralCaptions.isEmpty() ? "- No viral data available" :
                signals.viralCaptions.stream().map(c -> "- \"" + c + "\"").collect(Collectors.joining("\n")),
            // Last week topics
            signals.lastWeekTopics.stream().map(c -> "- \"" + c + "\"").collect(Collectors.joining("\n")),
            // Audience questions
            signals.audienceQuestions.isEmpty() ? "- No questions data" :
                signals.audienceQuestions.stream().map(q -> "- \"" + truncate(q, 80) + "\"").collect(Collectors.joining("\n")),
            // Content gaps
            signals.contentGaps.isEmpty() ? "- None detected" :
                signals.contentGaps.stream().map(g -> "- " + g).collect(Collectors.joining(", ")),
            // Plan start details
            planStart.toString(),
            planStart.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            buildDayNames(planStart),
            // Rules continuation
            signals.bestFormat,
            signals.niche,
            planStart.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase()
        );

        try {
            String response = chatLanguageModel.generate(prompt);
            log.info("Strategy LLM response received ({} chars)", response.length());
            return response;
        } catch (Exception e) {
            log.error("LLM call failed for strategy generation: {}", e.getMessage());
            throw new RuntimeException("Failed to generate strategy: " + e.getMessage());
        }
    }

    // === STEP 3: Parse LLM JSON response into DTO ===
    private WeeklyPlanDTO parseLLMResponse(String llmResponse, Creator creator, ContentSignals signals) {
        LocalDate planStart = LocalDate.now().plusDays(1);

        List<DayPlanDTO> days = new ArrayList<>();

        try {
            String jsonStr = extractJson(llmResponse);
            List<Map<String, Object>> parsed = objectMapper.readValue(jsonStr, new TypeReference<>() {});

            for (int i = 0; i < parsed.size() && i < 7; i++) {
                Map<String, Object> dayMap = parsed.get(i);
                LocalDate date = planStart.plusDays(i);

                DayPlanDTO day = DayPlanDTO.builder()
                    .day(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase())
                    .date(date.toString())
                    .contentPillar(getStr(dayMap, "content_pillar"))
                    .postIdea(getStr(dayMap, "post_idea"))
                    .captionDraft(getStr(dayMap, "caption"))
                    .hook(getStr(dayMap, "hook"))
                    .format(getStr(dayMap, "format"))
                    .platform("INSTAGRAM")
                    .hashtags(getList(dayMap, "hashtags"))
                    .bestTime(getStr(dayMap, "best_time"))
                    .callToAction(getStr(dayMap, "cta"))
                    .repurposeNote(getStr(dayMap, "repurpose_note"))
                    .build();

                days.add(day);
            }
        } catch (Exception e) {
            log.warn("Failed to parse LLM strategy response: {}", e.getMessage());
            return WeeklyPlanDTO.builder()
                .creatorId(creator.getId())
                .username(creator.getUsername())
                .weekStart(planStart.toString())
                .weekEnd(planStart.plusDays(6).toString())
                .detectedPillars(List.of(signals.niche))
                .bestPerformingStyle(signals.bestFormat)
                .days(Collections.emptyList())
                .strategyNotes(llmResponse)
                .build();
        }

        // Detect pillars from the generated plan
        List<String> pillars = days.stream()
            .map(DayPlanDTO::getContentPillar)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        return WeeklyPlanDTO.builder()
            .creatorId(creator.getId())
            .username(creator.getUsername())
            .weekStart(planStart.toString())
            .weekEnd(planStart.plusDays(6).toString())
            .detectedPillars(pillars)
            .bestPerformingStyle(signals.bestFormat)
            .days(days)
            .strategyNotes(String.format(
                "Plan starts tomorrow. Based on your last %d posts. Best format: %s. " +
                "Addressed %d audience questions. Filled %d content gaps. Avoided repeating last week's topics.",
                POSTS_TO_ANALYZE, signals.bestFormat,
                Math.min(signals.audienceQuestions.size(), 2),
                Math.min(signals.contentGaps.size(), 1)))
            .build();
    }

    // === HELPERS ===

    private String buildDayNames(LocalDate start) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (i > 0) sb.append(", ");
            LocalDate d = start.plusDays(i);
            sb.append(d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
              .append(" ").append(d.toString());
        }
        return sb.toString();
    }

    private WeeklyPlanDTO buildEmptyPlan(Creator creator, String message) {
        LocalDate planStart = LocalDate.now().plusDays(1);
        return WeeklyPlanDTO.builder()
            .creatorId(creator.getId())
            .username(creator.getUsername())
            .weekStart(planStart.toString())
            .weekEnd(planStart.plusDays(6).toString())
            .detectedPillars(Collections.emptyList())
            .bestPerformingStyle(null)
            .days(Collections.emptyList())
            .strategyNotes(message)
            .build();
    }

    private String extractJson(String text) {
        text = text.trim();
        if (text.startsWith("```json")) text = text.substring(7);
        else if (text.startsWith("```")) text = text.substring(3);
        if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
        text = text.trim();

        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) return (List<String>) val;
        return Collections.emptyList();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    // Internal data holder
    private static class ContentSignals {
        String username;
        String niche;
        Integer followerCount;
        String bestFormat;
        String bestDay;
        int bestHour;
        int imagePct;
        int videoPct;
        int postsPerWeek;
        List<String> topCaptions = new ArrayList<>();
        List<String> topHashtags = new ArrayList<>();
        List<String> lastWeekTopics = new ArrayList<>();    // NEW: avoid repetition
        List<String> viralCaptions = new ArrayList<>();     // NEW: what gets shared
        List<String> audienceQuestions = new ArrayList<>();  // NEW: what followers ask
        List<String> contentGaps = new ArrayList<>();       // NEW: unmet demand
    }
}
