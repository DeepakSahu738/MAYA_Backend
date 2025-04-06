package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceFacebook {

    @UserMessage("${FBPrompts.generatePostIdeas}")
    List<String> generatePostIdeas(
            @V("postGoal") String postGoal,
            @V("niche") String niche
    );

    @UserMessage("${FBPrompts.generateHeadlinesAndDes}")
    String generateHeadlinesAndDes(
            @V("postType") String postType,
            @V("toneStyle") String toneStyle,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${FBPrompts.suggestHashtags}")
    String suggestHashtags(
            @V("topicsAndKeywords") List<String> topicsAndKeywords
    );

    @UserMessage("${FBPrompts.suggestEngagementFeatures}")
    String suggestEngagementFeatures(
            @V("postType") String postType
    );

    @UserMessage("${FBPrompts.generateBoostingTips}")
    String generateBoostingTips(); // No parameters, so no @V needed

    @UserMessage("${FBPrompts.suggestBestPostTime}")
    String suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );
}
