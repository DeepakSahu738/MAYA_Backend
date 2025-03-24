package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServicePinterest {

    @UserMessage("${PinPrompts.generatePinIdeas}")
    List<String> generatePinIdeas(String pinGoal, String trendingOrEvergreen, String pinType,
                                    String targetAudience);


    @UserMessage("${PinPrompts.generateOptimizedTitleAndDescription}")
    String generateOptimizedTitleAndDescription(String searchKeywords,String callToAction);


    @UserMessage("${PinPrompts.suggestHashtag}")
    String suggestHashtag(String niche,String pinGoal);

    @UserMessage("${PinPrompts.suggestAestheticAndDesignTips}")
    String suggestAestheticAndDesignTips(String toneStyle,String pinType);

    @UserMessage("${PinPrompts.generateEngagement}")
    String generateEngagement(String niche, String callToAction);

    @UserMessage("${PinPrompts.suggestBestPostTime}")
    String suggestBestPostTime(String targetAudience);
}
