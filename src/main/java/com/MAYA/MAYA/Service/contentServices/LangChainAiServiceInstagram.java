package com.MAYA.MAYA.Service.contentServices;

import com.MAYA.MAYA.DTO.instagram.ResponsePOJOInstaContentIdea;
import com.MAYA.MAYA.DTO.instagram.ResponsePOJOInstaContentIdeaWRAPPER;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
 public interface LangChainAiServiceInstagram {

    //@SystemMessage("You are a polite assistant")
    String chat(String userMessage);

    @UserMessage("${instaprompts.generateContentIdeas}")
    ResponsePOJOInstaContentIdeaWRAPPER generateContentIdeas(@V("contentGoal") String contentGoal,
                                                             @V("niche") String niche,
                                                             @V("contentType") String contentType,
                                                             @V("trendingOrEvergreen") String trendingOrEvergreen,
                                                             @V("targetAudience") String targetAudience);

   @UserMessage("${instaprompts.generateCaptionWithCTA}")
   String generateCaption(
           @V("contentIdea") String contentIdea,
           @V("toneStyle") String toneStyle,
           @V("callToAction") String callToAction
   );

   @UserMessage("${instaprompts.suggestHashtags}")
   String generateHashtags(
           @V("contentIdea") String contentIdea,
           @V("niche") String niche,
           @V("keywords") List<String> keywords,
           @V("trendingOrEvergreen") String trendingOrEvergreen
   );

   @UserMessage("${instaprompts.suggestDesignAndAesthetic}")
   String generateDesignAndAesthetic(
           @V("contentIdea") String contentIdea,
           @V("niche") String niche,
           @V("toneStyle") String toneStyle,
           @V("contentType") String contentType
   );

   @UserMessage("${instaprompts.generateEngagementStrategies}")
   String generateEngagementStrategies(
           @V("contentIdea") String contentIdea,
           @V("contentGoal") String contentGoal,
           @V("targetAudience") String targetAudience
   );

   @UserMessage("${instaprompts.suggestBestPostTime}")
   String suggestBestPostTime(
           @V("contentIdea") String contentIdea,
           @V("targetAudience") String targetAudience,
           @V("niche") String niche
   );

}