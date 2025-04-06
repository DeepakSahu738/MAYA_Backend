package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceSnapchat {

    @UserMessage("${SCPrompts.generateStoryIdeas}")
    List<String> generateStoryIdeas(
            @V("snapGoal") String snapGoal,
            @V("niche") String niche,
            @V("storyType") String storyType,
            @V("toneStyle") String toneStyle,
            @V("callToAction") String callToAction,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${SCPrompts.generateTextOverlays}")
    String generateTextOverlays(
            @V("storyType") String storyType,
            @V("toneStyle") String toneStyle,
            @V("niche") String niche,
            @V("callToAction") String callToAction,
            @V("stickersAndFilters") List<String> stickersAndFilters,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${SCPrompts.suggestTrendingLensesAndFilters}")
    String suggestTrendingLensesAndFilters(
            @V("niche") String niche,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("storyType") String storyType,
            @V("targetAudience") String targetAudience,
            @V("stickersAndFilters") List<String> stickersAndFilters
    );

    @UserMessage("${SCPrompts.suggestEngagementFeatures}")
    String suggestEngagementFeatures(
            @V("toneStyle") String toneStyle,
            @V("storyType") String storyType,
            @V("niche") String niche
    );

    @UserMessage("${SCPrompts.generateBoostingTips}")
    String generateBoostingTips(); // No params — no @V needed

    @UserMessage("${SCPrompts.suggestBestPostTime}")
    String suggestBestPostTime(
            @V("targetAudience") String targetAudience,
            @V("storyType") String storyType
    );
}
