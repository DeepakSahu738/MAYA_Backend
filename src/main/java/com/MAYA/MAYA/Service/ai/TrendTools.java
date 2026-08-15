package com.MAYA.MAYA.Service.ai;

import com.MAYA.MAYA.DTO.analytics.CommentInsightDTO;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Entity.instagram.Post;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.PostRepository;
import com.MAYA.MAYA.Service.analytics.SnapshotAnalyticsService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Trend & Content Gap @Tool methods.
 *
 * Detects what the audience wants vs what the creator posts.
 * The gap = unmet content demand = post ideas.
 *
 * No external trend API needed — uses the creator's own data
 * (comment topics vs post captions) as the signal.
 */
@Component
@RequiredArgsConstructor
public class TrendTools {

    private final PostRepository postRepository;
    private final CreatorRepository creatorRepository;
    private final SnapshotAnalyticsService snapshotAnalyticsService;

    @Tool("Detect content gaps — topics the audience asks about in comments but the creator hasn't posted about recently")
    public String detectContentGap(@P("The creator's database ID") Long creatorId) {
        // What audience talks about (from comments)
        List<CommentInsightDTO> commentWords = snapshotAnalyticsService.getCommonCommentWords(creatorId, 30);

        // What creator posts about (from captions)
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        Set<String> captionWords = new HashSet<>();
        for (Post post : posts) {
            if (post.getCaption() != null) {
                String[] words = post.getCaption().toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+");
                for (String w : words) {
                    if (w.length() >= 4) captionWords.add(w);
                }
            }
        }

        // Gap = words in comments NOT in recent captions
        List<String> gaps = commentWords.stream()
            .filter(w -> !captionWords.contains(w.getWord().toLowerCase()))
            .limit(10)
            .map(w -> String.format("- \"%s\" (mentioned %d times in comments, never in your posts)",
                w.getWord(), w.getFrequency()))
            .collect(Collectors.toList());

        if (gaps.isEmpty()) {
            return "No significant content gaps detected — your posts align well with what your audience discusses!";
        }

        return "Content gaps detected — your audience talks about these topics but you haven't posted about them:\n" +
            String.join("\n", gaps) +
            "\n\nEach of these is a potential post idea. Want me to generate content ideas for any of them?";
    }

    @Tool("Analyze which content format and style went viral (high share rate) to suggest similar approaches")
    public String detectViralPatterns(@P("The creator's database ID") Long creatorId) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);

        // Find posts with highest share rate (viral signal)
        List<Post> viral = posts.stream()
            .filter(p -> p.getMetrics().getShares() != null && p.getMetrics().getReach() != null && p.getMetrics().getReach() > 0)
            .sorted((a, b) -> {
                double rateA = a.getMetrics().getShares() * 100.0 / a.getMetrics().getReach();
                double rateB = b.getMetrics().getShares() * 100.0 / b.getMetrics().getReach();
                return Double.compare(rateB, rateA);
            })
            .limit(5)
            .collect(Collectors.toList());

        if (viral.isEmpty()) return "Not enough share data to detect viral patterns.";

        // Analyze patterns
        long videoCount = viral.stream().filter(p -> "VIDEO".equalsIgnoreCase(p.getMediaType())).count();
        double avgCaptionLen = viral.stream().mapToInt(p -> p.getCaptionLength() != null ? p.getCaptionLength() : 0).average().orElse(0);

        StringBuilder sb = new StringBuilder("Viral content patterns (your most shared posts):\n\n");
        for (Post p : viral) {
            double shareRate = p.getMetrics().getShares() * 100.0 / p.getMetrics().getReach();
            sb.append(String.format("- \"%s\" (%s, share rate: %.1f%%)\n",
                truncate(p.getCaption(), 50), p.getMediaType(), shareRate));
        }
        sb.append(String.format("\nPatterns:\n- Format: %d/%d viral posts are VIDEO\n- Avg caption length: %.0f chars\n",
            videoCount, viral.size(), avgCaptionLen));
        sb.append("- Recommendation: Create more content in this style to maximize shares and organic reach.");

        return sb.toString();
    }

    @Tool("Get content ideas based on the creator's niche, audience interests, and performance data")
    public String suggestContentIdeas(@P("The creator's database ID") Long creatorId) {
        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        if (creator == null) return "Creator not found.";

        // Gather signals
        List<CommentInsightDTO> topWords = snapshotAnalyticsService.getCommonCommentWords(creatorId, 10);
        String niche = creator.getNiche() != null ? creator.getNiche() : "general";

        String audienceInterests = topWords.stream()
            .limit(5)
            .map(CommentInsightDTO::getWord)
            .collect(Collectors.joining(", "));

        return String.format(
            "Content idea context for the LLM to generate ideas:\n\n" +
            "Creator: @%s\nNiche: %s\nAudience top interests (from comments): %s\n\n" +
            "Based on this data, suggest 5 specific post ideas that:\n" +
            "1. Address what the audience is asking about\n" +
            "2. Fit the %s niche\n" +
            "3. Mix formats (some IMAGE, some VIDEO/REELS)\n" +
            "4. Include a hook, topic, and suggested CTA for each",
            creator.getUsername(), niche, audienceInterests, niche);
    }

    @Tool("Analyze hashtag effectiveness and suggest which to keep, drop, or try new ones")
    public String analyzeHashtagStrategy(@P("The creator's database ID") Long creatorId) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);

        // Group by hashtag and compute performance
        Map<String, List<Post>> byHashtag = new HashMap<>();
        for (Post post : posts) {
            if (post.getHashtags() == null) continue;
            for (String tag : post.getHashtags().split(",")) {
                tag = tag.trim();
                if (!tag.isEmpty()) {
                    byHashtag.computeIfAbsent(tag, k -> new ArrayList<>()).add(post);
                }
            }
        }

        // Find overused but underperforming
        List<String> toKeep = new ArrayList<>();
        List<String> toDrop = new ArrayList<>();

        for (Map.Entry<String, List<Post>> entry : byHashtag.entrySet()) {
            if (entry.getValue().size() < 3) continue; // need minimum data

            OptionalDouble avgEr = entry.getValue().stream()
                .map(Post::getMetrics)
                .filter(m -> m.getEngagementRate() != null)
                .mapToDouble(m -> m.getEngagementRate())
                .average();

            if (avgEr.isEmpty()) continue;

            if (avgEr.getAsDouble() > 15.0) {
                toKeep.add(String.format("%s (avg ER: %.1f%%, used %dx)", entry.getKey(), avgEr.getAsDouble(), entry.getValue().size()));
            } else if (entry.getValue().size() > 10 && avgEr.getAsDouble() < 10.0) {
                toDrop.add(String.format("%s (avg ER: %.1f%%, used %dx — underperforming)", entry.getKey(), avgEr.getAsDouble(), entry.getValue().size()));
            }
        }

        StringBuilder sb = new StringBuilder("Hashtag Strategy Analysis:\n\n");
        sb.append("KEEP (high engagement):\n");
        toKeep.stream().limit(5).forEach(h -> sb.append("  ✓ ").append(h).append("\n"));
        sb.append("\nCONSIDER DROPPING (overused, low engagement):\n");
        toDrop.stream().limit(5).forEach(h -> sb.append("  ✗ ").append(h).append("\n"));

        if (toKeep.isEmpty() && toDrop.isEmpty()) {
            return "Not enough hashtag performance data to analyze. Need more posts with reach data.";
        }

        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
