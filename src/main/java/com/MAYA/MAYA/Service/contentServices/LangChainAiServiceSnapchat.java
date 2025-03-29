package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceSnapchat {

    @UserMessage("${SCPrompts.generateStoryIdeas}")
    List<String> generateStoryIdeas(String snapGoal, String niche,String storyType,
                                    String toneStyle,String callToAction,String trendingOrEvergreen);


    @UserMessage("${SCPrompts.generateTextOverlays}")
    String generateTextOverlays(String storyType,String toneStyle,String niche,String callToAction,
                                List<String>stickersAndFilters , String targetAudience);


    @UserMessage("${SCPrompts.suggestTrendingLensesAndFilters}")
    String suggestTrendingLensesAndFilters(String niche,String trendingOrEvergreen,String storyType,
                                           String targetAudience, List<String> stickersAndFilters);

    @UserMessage("${SCPrompts.suggestEngagementFeatures}")
    String suggestEngagementFeatures(String toneStyle,String storyType,String niche);

    @UserMessage("${SCPrompts.generateBoostingTips}")
    String generateBoostingTips();

    @UserMessage("${SCPrompts.suggestBestPostTime}")
    String suggestBestPostTime(String targetAudience,String storyType);
}
