package com.MAYA.MAYA.Service.ai;

import com.MAYA.MAYA.Entity.instagram.Comment;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Repository.instagram.CommentRepository;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comment @Tool methods — surfaces unanswered questions and drafts replies.
 *
 * Note: draftReply uses the LLM itself (the orchestrator generates the reply
 * based on the comment context we provide here). We just supply the data.
 */
@Component
@RequiredArgsConstructor
public class CommentTools {

    private final CommentRepository commentRepository;
    private final CreatorRepository creatorRepository;

    @Tool("Get the top unanswered questions from comments that the creator hasn't replied to yet")
    public String getUnansweredQuestions(@P("The creator's database ID") Long creatorId) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);

        List<Comment> unanswered = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()))
            .filter(c -> c.getReplyCount() == null || c.getReplyCount() == 0)
            .sorted(Comparator.comparingInt(c -> -(c.getLikeCount() != null ? c.getLikeCount() : 0)))
            .limit(10)
            .collect(Collectors.toList());

        if (unanswered.isEmpty()) return "No unanswered questions found — great job staying engaged!";

        return "Top unanswered questions (sorted by likes — highest priority):\n" +
            unanswered.stream()
                .map(c -> String.format("- \"%s\" by @%s (%d likes)",
                    truncate(c.getText(), 80), c.getUsername(),
                    c.getLikeCount() != null ? c.getLikeCount() : 0))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Get context about a specific comment to help draft a reply — provides the comment text, who wrote it, and the creator's tone style")
    public String getCommentContext(
            @P("The creator's database ID") Long creatorId,
            @P("The comment text to reply to") String commentText) {

        Creator creator = creatorRepository.findById(creatorId).orElse(null);
        if (creator == null) return "Creator not found.";

        String tone = creator.getToneStyle() != null ? creator.getToneStyle() : "friendly and helpful";
        String niche = creator.getNiche() != null ? creator.getNiche() : "general";

        return String.format(
            "Draft a reply to this comment:\n\nComment: \"%s\"\n\n" +
            "Creator info:\n- Username: @%s\n- Niche: %s\n- Tone: %s\n\n" +
            "Guidelines:\n- Keep it short (1-2 sentences)\n- Match the creator's tone\n- Be helpful and engaging\n- If it's a question, answer it directly\n- Add a relevant emoji if appropriate",
            commentText, creator.getUsername(), niche, tone);
    }

    @Tool("Get recent comments with high engagement (most liked comments) to understand audience sentiment")
    public String getHighEngagementComments(@P("The creator's database ID") Long creatorId) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);

        List<Comment> topComments = comments.stream()
            .sorted(Comparator.comparingInt(c -> -(c.getLikeCount() != null ? c.getLikeCount() : 0)))
            .limit(10)
            .collect(Collectors.toList());

        if (topComments.isEmpty()) return "No comments found.";

        return "Most liked comments (audience favorites):\n" +
            topComments.stream()
                .map(c -> String.format("- \"%s\" by @%s (%d likes, %s)",
                    truncate(c.getText(), 60), c.getUsername(),
                    c.getLikeCount() != null ? c.getLikeCount() : 0,
                    Boolean.TRUE.equals(c.getIsQuestion()) ? "QUESTION" : "statement"))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Get comment statistics — total comments, questions count, unanswered count, and sentiment split")
    public String getCommentStats(@P("The creator's database ID") Long creatorId) {
        List<Comment> comments = commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId);
        if (comments.isEmpty()) return "No comments found.";

        long total = comments.size();
        long questions = comments.stream().filter(c -> Boolean.TRUE.equals(c.getIsQuestion())).count();
        long unanswered = comments.stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsQuestion()) && (c.getReplyCount() == null || c.getReplyCount() == 0))
            .count();

        // Basic sentiment from stored data
        long positive = comments.stream().filter(c -> "POSITIVE".equalsIgnoreCase(c.getSentiment())).count();
        long negative = comments.stream().filter(c -> "NEGATIVE".equalsIgnoreCase(c.getSentiment())).count();

        return String.format(
            "Comment Stats:\n- Total: %d\n- Questions: %d (%.1f%%)\n- Unanswered questions: %d\n- Positive sentiment: %d\n- Negative sentiment: %d\n- Response rate: %.1f%%",
            total, questions, questions * 100.0 / total, unanswered,
            positive, negative,
            questions > 0 ? (questions - unanswered) * 100.0 / questions : 0);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
