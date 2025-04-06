package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceTikTok {

    @UserMessage("${TTPrompts.generateVideoIdeas}")
    List<String> generateVideoIdeas(
            @V("niche") String niche,
            @V("videoFormat") String videoFormat,
            @V("videoGoal") String videoGoal,
            @V("targetAudience") String targetAudience,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${TTPrompts.generateHooksAndCaption}")
    String generateHooksAndCaption(
            @V("toneStyle") String toneStyle,
            @V("callToAction") String callToAction,
            @V("niche") String niche
    );

    @UserMessage("${TTPrompts.suggestHashtag}")
    String suggestHashtag(
            @V("videoFormat") String videoFormat,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("niche") String niche,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${TTPrompts.suggestMusicAndEffect}")
    String suggestMusicAndEffect(
            @V("toneStyle") String toneStyle,
            @V("videoFormat") String videoFormat,
            @V("niche") String niche
    );

    @UserMessage("${TTPrompts.generateEngagement}")
    String generateEngagement(
            @V("videoGoal") String videoGoal,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${TTPrompts.suggestBestPostTime}")
    String suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );

}
