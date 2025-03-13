package com.MAYA.MAYA.Service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@AiService
 public interface demoLangChainService {

    //@SystemMessage("You are a polite assistant")
    String chat(String userMessage);

    @UserMessage("${prompts.generateContentIdeas}")
    List<String>  generateContentIdeas(String contentGoal, String niche,String contentType,
                                String trendingOrEvergreen,String targetAudience);


    @UserMessage("${prompts.generateCaptionWithCTA}")
    String generateCaption(String contentIdea,String toneStyle,String callToAction);


    @UserMessage("${prompts.suggestHashtags}")
    String generateHashtags(String contentIdea,String niche, List<String> keywords, String trendingOrEvergreen);

    @UserMessage("${prompts.suggestDesignAndAesthetic}")
    String generateDesignAndAesthetic( String contentIdea,String niche,
                               String toneStyle,
                               String contentType);

    @UserMessage("${prompts.generateEngagementStrategies}")
    String generateEngagementStrategies(String contentIdea,String contentGoal,String targetAudience);

    @UserMessage("${prompts.suggestBestPostTime}")
    String suggestBestPostTime(String contentIdea,String targetAudience,String niche);

}