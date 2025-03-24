package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceTikTok {

    @UserMessage("${TTPrompts.generateVideoIdeas}")
    List<String> generateVideoIdeas(String niche, String videoFormat, String videoGoal,
                                    String targetAudience, String trendingOrEvergreen);


    @UserMessage("${TTPrompts.generateHooksAndCaption}")
    String generateHooksAndCaption(String toneStyle,String callToAction,String niche);


    @UserMessage("${TTPrompts.suggestHashtag}")
    String suggestHashtag(String videoFormat,String trendingOrEvergreen,String niche,
                                           String targetAudience, String stickersAndFilters);

    @UserMessage("${TTPrompts.suggestMusicAndEffect}")
    String suggestMusicAndEffect(String toneStyle,String videoFormat,String niche);

    @UserMessage("${TTPrompts.generateEngagement}")
    String generateEngagement(String videoGoal, String targetAudience);

    @UserMessage("${TTPrompts.suggestBestPostTime}")
    String suggestBestPostTime(String targetAudience);
}
