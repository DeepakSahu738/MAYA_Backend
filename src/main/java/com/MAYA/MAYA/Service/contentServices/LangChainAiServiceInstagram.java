package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
 public interface LangChainAiServiceInstagram {

    //@SystemMessage("You are a polite assistant")
    String chat(String userMessage);

    @UserMessage("${instaprompts.generateContentIdeas}")
    List<String>  generateContentIdeas(String contentGoal, String niche,String contentType,
                                String trendingOrEvergreen,String targetAudience);


    @UserMessage("${instaprompts.generateCaptionWithCTA}")
    String generateCaption(String contentIdea,String toneStyle,String callToAction);


    @UserMessage("${instaprompts.suggestHashtags}")
    String generateHashtags(String contentIdea,String niche, List<String> keywords, String trendingOrEvergreen);

    @UserMessage("${instaprompts.suggestDesignAndAesthetic}")
    String generateDesignAndAesthetic( String contentIdea,String niche,
                               String toneStyle,
                               String contentType);

    @UserMessage("${instaprompts.generateEngagementStrategies}")
    String generateEngagementStrategies(String contentIdea,String contentGoal,String targetAudience);

    @UserMessage("${instaprompts.suggestBestPostTime}")
    String suggestBestPostTime(String contentIdea,String targetAudience,String niche);

}