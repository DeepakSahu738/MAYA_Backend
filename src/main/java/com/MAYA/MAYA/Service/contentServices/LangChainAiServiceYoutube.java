package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceYoutube {

    @UserMessage("${YTPrompts.generateVideoIdeas}")
    List<String> generateVideoIdeas(
            @V("videoGoal") String videoGoal,
            @V("niche") String niche,
            @V("videoType") String videoType,
            @V("targetAudience") String targetAudience,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${YTPrompts.generateSEO}")
    String generateSEO(
            @V("keywordsAndSeoTags") List<String> keywordsAndSeoTags,
            @V("callToAction") String callToAction
    );

    @UserMessage("${YTPrompts.suggestHashtag}")
    String suggestHashtag(
            @V("keywordsAndSeoTags") List<String> keywordsAndSeoTags,
            @V("niche") String niche,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${YTPrompts.suggestThumbnailAndBranding}")
    String suggestThumbnailAndBranding(
            @V("toneStyle") String toneStyle,
            @V("niche") String niche,
            @V("videoGoal") String videoGoal
    );

    @UserMessage("${YTPrompts.generateEngagement}")
    String generateEngagement(
            @V("targetAudience") String targetAudience,
            @V("niche") String niche
    );

    @UserMessage("${YTPrompts.suggestBestPostTime}")
    String suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );

}
