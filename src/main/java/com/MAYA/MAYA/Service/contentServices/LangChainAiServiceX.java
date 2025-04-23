package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceX {

    @UserMessage("${XPrompts.generateTweetIdeas}")
    List<String> generateTweetIdeas(
            @V("tweetGoal") String tweetGoal,
            @V("niche") String niche,
            @V("targetAudience") String targetAudience,
            @V("tweetType") String tweetType,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${XPrompts.generateOptimizedTweetCopy}")
    String generateOptimizedTweetCopy(
            @V("toneStyle") String toneStyle,
            @V("hashtagsAndMentions") List<String> hashtagsAndMentions,
            @V("callToAction") String callToAction,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${XPrompts.suggestHashtag}")
    String suggestHashtag(
            @V("hashtagsAndMentions") List<String> hashtagsAndMentions,
            @V("niche") String niche,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${XPrompts.suggestVisualAndGIFSuggestions}")
    String suggestVisualAndGIFSuggestions(
            @V("tweetType") String tweetType,
            @V("toneStyle") String toneStyle,
            @V("niche") String niche,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${XPrompts.generateEngagement}")
    String generateEngagement(
            @V("targetAudience") String targetAudience,
            @V("niche") String niche
    );

    @UserMessage("${XPrompts.suggestBestPostTime}")
    String suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );

}
