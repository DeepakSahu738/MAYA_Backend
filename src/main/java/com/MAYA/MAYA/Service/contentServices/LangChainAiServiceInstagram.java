package com.MAYA.MAYA.Service.contentServices;

import com.MAYA.MAYA.DTO.instagram.captionPojo.ResponsePOJOInstaCaptionWRAPPER;
import com.MAYA.MAYA.DTO.instagram.contentIdeaPojo.ResponsePOJOInstaContentIdeaWRAPPER;
import com.MAYA.MAYA.DTO.instagram.designAndAestheticPojo.ResponsePOJOInstaDesignAndAestheticWRAPPER;
import com.MAYA.MAYA.DTO.instagram.engagementPojo.ResponsePOJOInstaEngagementStrategiesWRAPPER;
import com.MAYA.MAYA.DTO.instagram.hashtagsPojo.ResponsePOJOInstaHashtagsWRAPPER;
import com.MAYA.MAYA.DTO.instagram.postingTimePojo.ResponsePOJOInstaBestPostingTimeWRAPPER;
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
   ResponsePOJOInstaCaptionWRAPPER generateCaption(
           @V("niche") String niche,
           @V("toneStyle") String toneStyle,
           @V("callToAction") String callToAction
   );

   @UserMessage("${instaprompts.suggestHashtags}")
   ResponsePOJOInstaHashtagsWRAPPER generateHashtags(
           @V("niche") String niche,
           @V("keywords") List<String> keywords,
           @V("trendingOrEvergreen") String trendingOrEvergreen
   );

   @UserMessage("${instaprompts.suggestDesignAndAesthetic}")
   ResponsePOJOInstaDesignAndAestheticWRAPPER generateDesignAndAesthetic(
           @V("niche") String niche,
           @V("toneStyle") String toneStyle,
           @V("contentType") String contentType
   );

   @UserMessage("${instaprompts.generateEngagementStrategies}")
   ResponsePOJOInstaEngagementStrategiesWRAPPER generateEngagementStrategies(
           @V("contentGoal") String contentGoal,
           @V("targetAudience") String targetAudience
   );

   @UserMessage("${instaprompts.suggestBestPostTime}")
   ResponsePOJOInstaBestPostingTimeWRAPPER suggestBestPostTime(
           @V("targetAudience") String targetAudience,
           @V("niche") String niche
   );

}