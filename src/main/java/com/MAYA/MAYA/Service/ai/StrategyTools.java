package com.MAYA.MAYA.Service.ai;

import com.MAYA.MAYA.DTO.strategy.WeeklyPlanDTO;
import com.MAYA.MAYA.DTO.strategy.WeeklyPlanDTO.DayPlanDTO;
import com.MAYA.MAYA.Service.strategy.WeeklyStrategyService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Strategy @Tool methods — allows the AI chat to generate content plans.
 * User can say: "Build my content plan for next week" in chat.
 */
@Component
@RequiredArgsConstructor
public class StrategyTools {

    private final WeeklyStrategyService strategyService;

    @Tool("Generate a personalized 7-day content plan based on the creator's recent posts, performance data, and content style")
    public String generateWeeklyPlan(@P("The creator's database ID") Long creatorId) {
        try {
            WeeklyPlanDTO plan = strategyService.generateWeeklyPlan(creatorId);

            if (plan.getDays() == null || plan.getDays().isEmpty()) {
                return "Could not generate a plan: " + plan.getStrategyNotes();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📅 Weekly Content Plan (%s to %s)\n", plan.getWeekStart(), plan.getWeekEnd()));
            sb.append(String.format("Content pillars detected: %s\n", String.join(", ", plan.getDetectedPillars())));
            sb.append(String.format("Best performing style: %s\n\n", plan.getBestPerformingStyle()));

            for (DayPlanDTO day : plan.getDays()) {
                sb.append(String.format("--- %s (%s) ---\n", day.getDay(), day.getDate()));
                sb.append(String.format("Pillar: %s | Format: %s | Time: %s\n", day.getContentPillar(), day.getFormat(), day.getBestTime()));
                sb.append(String.format("Idea: %s\n", day.getPostIdea()));
                sb.append(String.format("Hook: \"%s\"\n", day.getHook()));
                sb.append(String.format("Caption: %s\n", truncate(day.getCaptionDraft(), 150)));
                if (day.getHashtags() != null && !day.getHashtags().isEmpty()) {
                    sb.append(String.format("Hashtags: %s\n", String.join(" ", day.getHashtags())));
                }
                sb.append(String.format("CTA: %s\n", day.getCallToAction()));
                sb.append(String.format("Repurpose: %s\n\n", day.getRepurposeNote()));
            }

            sb.append("💡 ").append(plan.getStrategyNotes());
            sb.append("\n\nWant me to save this plan to your calendar?");

            return sb.toString();
        } catch (Exception e) {
            return "Failed to generate plan: " + e.getMessage() + ". Make sure you have synced posts.";
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
