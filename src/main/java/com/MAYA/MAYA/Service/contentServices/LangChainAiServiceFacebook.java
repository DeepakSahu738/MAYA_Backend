package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceFacebook {

    @UserMessage("${FBPrompts.generatePostIdeas}")
    List<String> generatePostIdeas(String postGoal, String niche);


    @UserMessage("${FBPrompts.generateHeadlinesAndDes}")
    String generateHeadlinesAndDes(String postType,String toneStyle,String targetAudience);


    @UserMessage("${FBPrompts.suggestHashtags}")
    String suggestHashtags(List<String> topicsAndKeywords);

    @UserMessage("${FBPrompts.suggestEngagementFeatures}")
    String suggestEngagementFeatures(String postType);

    @UserMessage("${FBPrompts.generateBoostingTips}")
    String generateBoostingTips();

    @UserMessage("${FBPrompts.suggestBestPostTime}")
    String suggestBestPostTime(String targetAudience);
}
