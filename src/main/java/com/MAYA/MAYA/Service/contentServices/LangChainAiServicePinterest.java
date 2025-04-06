package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServicePinterest {
    @UserMessage("${PinPrompts.generatePinIdeas}")
    List<String> generatePinIdeas(
            @V("pinGoal") String pinGoal,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("pinType") String pinType,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${PinPrompts.generateOptimizedTitleAndDescription}")
    String generateOptimizedTitleAndDescription(
            @V("searchKeywords") List<String> searchKeywords,
            @V("callToAction") String callToAction
    );

    @UserMessage("${PinPrompts.suggestHashtag}")
    String suggestHashtag(
            @V("niche") String niche,
            @V("pinGoal") String pinGoal
    );

    @UserMessage("${PinPrompts.suggestAestheticAndDesignTips}")
    String suggestAestheticAndDesignTips(
            @V("toneStyle") String toneStyle,
            @V("pinType") String pinType
    );

    @UserMessage("${PinPrompts.generateEngagement}")
    String generateEngagement(
            @V("niche") String niche,
            @V("callToAction") String callToAction
    );

    @UserMessage("${PinPrompts.suggestBestPostTime}")
    String suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );
}
