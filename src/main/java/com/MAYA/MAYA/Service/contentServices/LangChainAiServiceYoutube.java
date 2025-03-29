package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceYoutube {

    @UserMessage("${YTPrompts.generateVideoIdeas}")
    List<String> generateVideoIdeas(String videoGoal, String niche, String videoType,
                                    String targetAudience, String trendingOrEvergreen);


    @UserMessage("${YTPrompts.generateSEO}")
    String generateSEO(List<String> keywordsAndSeoTags,String callToAction);


    @UserMessage("${YTPrompts.suggestHashtag}")
    String suggestHashtag(List<String> keywordsAndSeoTags,String niche,String trendingOrEvergreen);

    @UserMessage("${YTPrompts.suggestThumbnailAndBranding}")
    String suggestThumbnailAndBranding(String toneStyle,String niche,String videoGoal);

    @UserMessage("${YTPrompts.generateEngagement}")
    String generateEngagement(String targetAudience, String niche);

    @UserMessage("${YTPrompts.suggestBestPostTime}")
    String suggestBestPostTime(String targetAudience);
}
