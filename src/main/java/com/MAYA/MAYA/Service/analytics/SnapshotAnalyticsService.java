package com.MAYA.MAYA.Service.analytics;

import com.MAYA.MAYA.DTO.analytics.*;
import com.MAYA.MAYA.Entity.instagram.Comment;
import com.MAYA.MAYA.Entity.instagram.Post;
import com.MAYA.MAYA.Entity.instagram.PostMetrics;
import com.MAYA.MAYA.Repository.instagram.CommentRepository;
import com.MAYA.MAYA.Repository.instagram.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Snapshot-only metrics — current state rankings, no week-over-week delta.
 * Pure Java math only. NEVER calls LLM.
 */
@Service
@RequiredArgsConstructor
public class SnapshotAnalyticsService {
    
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    
    // ===== Metric #17: Best Performing Posts =====
    public List<PostPerformanceDTO> getBestPerformingPosts(Long creatorId, Integer limit) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        return posts.stream()
            .filter(p -> p.getMetrics().getReach() != null && p.getMetrics().getReach() > 0)
            .sorted((a, b) -> Double.compare(
                computeEngagementRate(b.getMetrics()),
                computeEngagementRate(a.getMetrics())))
            .limit(limit != null ? limit : 5)
            .map(this::toPostPerformanceDTO)
            .collect(Collectors.toList());
    }
    
    // ===== Metric #17: Worst Performing Posts =====
    public List<PostPerformanceDTO> getWorstPerformingPosts(Long creatorId, Integer limit) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        return posts.stream()
            .filter(p -> p.getMetrics().getReach() != null && p.getMetrics().getReach() > 0)
            .sorted(Comparator.comparingDouble(p -> computeEngagementRate(p.getMetrics())))
            .limit(limit != null ? limit : 5)
            .map(this::toPostPerformanceDTO)
            .collect(Collectors.toList());
    }
    
    // ===== Metric #18: Most-Used Hashtags Ranked =====
    public List<HashtagPerformanceDTO> getMostUsedHashtags(Long creatorId, Integer limit) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        // Count usage per hashtag
        Map<String, Integer> usageCount = new HashMap<>();
        Map<String, Integer> totalLikes = new HashMap<>();
        
        for (Post post : posts) {
            List<String> tags = parseHashtags(post.getHashtags());
            for (String tag : tags) {
                usageCount.merge(tag, 1, Integer::sum);
                totalLikes.merge(tag, post.getMetrics().getLikes(), Integer::sum);
            }
        }
        
        return usageCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit != null ? limit : 20)
            .map(e -> new HashtagPerformanceDTO(
                e.getKey(),
                e.getValue(),
                null, // avg engagement computed in #19
                null,
                totalLikes.getOrDefault(e.getKey(), 0)
            ))
            .collect(Collectors.toList());
    }
    
    // ===== Metric #19: Top Performing Hashtags =====
    public List<HashtagPerformanceDTO> getTopPerformingHashtags(Long creatorId, Integer limit) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        // Group posts by hashtag
        Map<String, List<Post>> postsByHashtag = new HashMap<>();
        for (Post post : posts) {
            List<String> tags = parseHashtags(post.getHashtags());
            for (String tag : tags) {
                postsByHashtag.computeIfAbsent(tag, k -> new ArrayList<>()).add(post);
            }
        }
        
        // Compute performance score per hashtag (min 3 posts)
        List<HashtagPerformanceDTO> results = new ArrayList<>();
        for (Map.Entry<String, List<Post>> entry : postsByHashtag.entrySet()) {
            List<Post> tagPosts = entry.getValue();
            if (tagPosts.size() < 3) continue;
            
            OptionalDouble avgReach = tagPosts.stream()
                .map(Post::getMetrics)
                .filter(m -> m.getReach() != null)
                .mapToDouble(PostMetrics::getReach)
                .average();
            
            OptionalDouble avgEr = tagPosts.stream()
                .map(Post::getMetrics)
                .filter(m -> m.getReach() != null && m.getReach() > 0)
                .mapToDouble(this::computeEngagementRate)
                .average();
            
            int totalLikes = tagPosts.stream()
                .mapToInt(p -> p.getMetrics().getLikes() != null ? p.getMetrics().getLikes() : 0)
                .sum();
            
            results.add(new HashtagPerformanceDTO(
                entry.getKey(),
                tagPosts.size(),
                avgEr.isPresent() ? round(avgEr.getAsDouble()) : null,
                avgReach.isPresent() ? round(avgReach.getAsDouble()) : null,
                totalLikes
            ));
        }
        
        // Sort by performance_score = (avg_reach × 0.4) + (avg_er × 0.6)
        results.sort((a, b) -> {
            double scoreA = (a.getAvgReach() != null ? a.getAvgReach() * 0.4 : 0)
                + (a.getAvgEngagementRate() != null ? a.getAvgEngagementRate() * 0.6 : 0);
            double scoreB = (b.getAvgReach() != null ? b.getAvgReach() * 0.4 : 0)
                + (b.getAvgEngagementRate() != null ? b.getAvgEngagementRate() * 0.6 : 0);
            return Double.compare(scoreB, scoreA);
        });
        
        return results.stream()
            .limit(limit != null ? limit : 10)
            .collect(Collectors.toList());
    }
    
    // ===== Metric #20: Caption Length vs Engagement Correlation =====
    public SnapshotMetricDTO getCaptionLengthCorrelation(Long creatorId) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        // Bucket: SHORT (<100), MEDIUM (100-300), LONG (>300)
        Map<String, List<Post>> buckets = new HashMap<>();
        buckets.put("SHORT", new ArrayList<>());
        buckets.put("MEDIUM", new ArrayList<>());
        buckets.put("LONG", new ArrayList<>());
        
        for (Post post : posts) {
            int len = post.getCaptionLength() != null ? post.getCaptionLength() : 0;
            if (len < 100) buckets.get("SHORT").add(post);
            else if (len <= 300) buckets.get("MEDIUM").add(post);
            else buckets.get("LONG").add(post);
        }
        
        // Find best bucket (min 5 posts for valid comparison)
        Map<String, Double> bucketAvgEr = new HashMap<>();
        for (Map.Entry<String, List<Post>> entry : buckets.entrySet()) {
            if (entry.getValue().size() >= 5) {
                OptionalDouble avg = entry.getValue().stream()
                    .map(Post::getMetrics)
                    .filter(m -> m.getEngagementRate() != null)
                    .mapToDouble(PostMetrics::getEngagementRate)
                    .average();
                if (avg.isPresent()) {
                    bucketAvgEr.put(entry.getKey(), round(avg.getAsDouble()));
                }
            }
        }
        
        String bestBucket = bucketAvgEr.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("SHORT");
        
        return new SnapshotMetricDTO("caption_length_best_bucket", bestBucket, LocalDateTime.now());
    }
    
    // ===== Metric #21: Day & Hour Recommendation =====
    public DayHourRecommendationDTO getBestDayHourToPost(Long creatorId) {
        List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);
        
        // Group by (day, hour) — only slots with >= 2 posts
        Map<String, List<Post>> slotPosts = new HashMap<>();
        Map<DayOfWeek, List<Post>> dayPosts = new HashMap<>();
        Map<Integer, List<Post>> hourPosts = new HashMap<>();
        
        for (Post post : posts) {
            DayOfWeek day = post.getPostedAt().getDayOfWeek();
            int hour = post.getPostedAt().getHour();
            String slot = day.name() + "_" + hour;
            
            slotPosts.computeIfAbsent(slot, k -> new ArrayList<>()).add(post);
            dayPosts.computeIfAbsent(day, k -> new ArrayList<>()).add(post);
            hourPosts.computeIfAbsent(hour, k -> new ArrayList<>()).add(post);
        }
        
        // Best day (highest avg engagement across all hours)
        String bestDay = dayPosts.entrySet().stream()
            .max(Comparator.comparingDouble(e -> avgEngagementForPosts(e.getValue())))
            .map(e -> e.getKey().name())
            .orElse("MONDAY");
        
        // Best hour (highest avg engagement across all days)
        Integer bestHour = hourPosts.entrySet().stream()
            .max(Comparator.comparingDouble(e -> avgEngagementForPosts(e.getValue())))
            .map(Map.Entry::getKey)
            .orElse(9);
        
        // Best slot's engagement rate
        Double bestSlotEr = slotPosts.entrySet().stream()
            .filter(e -> e.getValue().size() >= 2)
            .max(Comparator.comparingDouble(e -> avgEngagementForPosts(e.getValue())))
            .map(e -> round(avgEngagementForPosts(e.getValue())))
            .orElse(null);
        
        return new DayHourRecommendationDTO(bestDay, bestHour, bestSlotEr, "IST");
    }
    
    // ===== Metric #22: Most Common Words in Comments =====
    public List<CommentInsightDTO> getCommonCommentWords(Long creatorId, Integer limit) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);
        
        Set<String> stopwords = Set.of(
            "a", "an", "the", "is", "it", "i", "you", "this", "that", "for", "with",
            "and", "or", "but", "in", "on", "at", "to", "of", "so", "not", "no",
            "my", "me", "your", "we", "he", "she", "they", "are", "was", "were",
            "can", "do", "did", "has", "have", "had", "been", "will", "would",
            "should", "could", "just", "very", "really", "too", "also", "more",
            "all", "from", "how", "what", "when", "where", "which", "who"
        );
        
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        for (Comment comment : comments) {
            String text = comment.getText().toLowerCase()
                .replaceAll("[^a-z\\s]", "") // remove punctuation and emojis
                .trim();
            
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (word.length() >= 3 && !stopwords.contains(word)) {
                    wordFrequency.merge(word, 1, Integer::sum);
                }
            }
        }
        
        return wordFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit != null ? limit : 30)
            .map(e -> new CommentInsightDTO(e.getKey(), e.getValue(), null))
            .collect(Collectors.toList());
    }
    
    // ===== Metric #23: Top Commenters / Superfans =====
    public List<TopCommenterDTO> getTopCommenters(Long creatorId, Integer limit) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);
        
        // Group by commenter username
        Map<String, List<Comment>> byUser = comments.stream()
            .collect(Collectors.groupingBy(Comment::getUsername));
        
        return byUser.entrySet().stream()
            .map(e -> {
                String username = e.getKey();
                List<Comment> userComments = e.getValue();
                int commentCount = userComments.size();
                int totalLikes = userComments.stream()
                    .mapToInt(c -> c.getLikeCount() != null ? c.getLikeCount() : 0)
                    .sum();
                // superfan_score = (comment_count × 2) + (total_likes_received × 0.5)
                double superfanScore = (commentCount * 2.0) + (totalLikes * 0.5);
                return new TopCommenterDTO(username, commentCount, superfanScore);
            })
            .sorted((a, b) -> Double.compare(b.getAvgSentimentScore(), a.getAvgSentimentScore()))
            .limit(limit != null ? limit : 5)
            .collect(Collectors.toList());
    }
    
    // ======================================================================
    // HELPER METHODS
    // ======================================================================
    
    private double computeEngagementRate(PostMetrics m) {
        if (m.getReach() == null || m.getReach() == 0) return 0.0;
        int likes = m.getLikes() != null ? m.getLikes() : 0;
        int comments = m.getComments() != null ? m.getComments() : 0;
        int saves = m.getSaves() != null ? m.getSaves() : 0;
        int shares = m.getShares() != null ? m.getShares() : 0;
        int totalEngagement = likes + comments + saves + shares;
        // If reach < total engagement, data is unreliable — return 0
        if (m.getReach() < totalEngagement) return 0.0;
        double er = totalEngagement * 100.0 / m.getReach();
        return Math.min(er, 100.0); // cap at 100%
    }
    
    private double avgEngagementForPosts(List<Post> posts) {
        return posts.stream()
            .map(Post::getMetrics)
            .filter(m -> m != null && m.getReach() != null && m.getReach() > 0)
            .mapToDouble(this::computeEngagementRate)
            .filter(er -> er <= 100.0) // exclude unreliable data
            .average()
            .orElse(0.0);
    }
    
    private PostPerformanceDTO toPostPerformanceDTO(Post post) {
        PostMetrics m = post.getMetrics();
        return new PostPerformanceDTO(
            post.getId(),
            post.getInstagramId(),
            post.getCaption(),
            post.getMediaType(),
            round(computeEngagementRate(m)),
            m.getLikes(),
            m.getComments(),
            m.getSaves(),
            post.getPostedAt()
        );
    }
    
    private List<String> parseHashtags(String hashtags) {
        if (hashtags == null || hashtags.isBlank()) return Collections.emptyList();
        return Arrays.stream(hashtags.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
