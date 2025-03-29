package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceX {

    @UserMessage("${XPrompts.generateTweetIdeas}")
    List<String> generateTweetIdeas(String tweetGoal, String niche, String targetAudience,
                                    String tweetType, String trendingOrEvergreen);


    @UserMessage("${XPrompts.generateOptimizedTweetCopy}")
    String generateOptimizedTweetCopy(String toneStyle,List<String> hashtagsAndMentions,String callToAction,
                                      String targetAudience);


    @UserMessage("${XPrompts.suggestHashtag}")
    String suggestHashtag(List<String> hashtagsAndMentions,String niche,String trendingOrEvergreen);

    @UserMessage("${XPrompts.suggestVisualAndGIFSuggestions}")
    String suggestVisualAndGIFSuggestions(String tweetType,String toneStyle,String niche,String targetAudience);

    @UserMessage("${XPrompts.generateEngagement}")
    String generateEngagement(String targetAudience, String niche);

    @UserMessage("${XPrompts.suggestBestPostTime}")
    String suggestBestPostTime(String targetAudience);
}
