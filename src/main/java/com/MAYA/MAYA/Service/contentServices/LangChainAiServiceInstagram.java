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

//    @UserMessage("${instaprompts.generateContentIdeas}")
//    ResponsePOJOInstaContentIdeaWRAPPER generateContentIdeas(@V("contentGoal") String contentGoal,
//                                                             @V("niche") String niche,
//                                                             @V("contentType") String contentType,
//                                                             @V("trendingOrEvergreen") String trendingOrEvergreen,
//                                                             @V("targetAudience") String targetAudience);
    @UserMessage("Generate 15 unique and engaging content ideas for the goal {{contentGoal}} in the niche {{niche}}. The content type is {{contentType}}, targeting {{targetAudience}}, and should be {{trendingOrEvergreen}} in style.Respond ONLY in a strict JSON array with the following format:[{\"contentIdea\": \"Your idea here\",\"script\": \"Your short script or caption here\",\"whyThisWorks\": \"Explain why this idea will resonate with the target audience\"},...]Do not include any introductory or explanatory text. Only return valid JSON. Generate 15")
    ResponsePOJOInstaContentIdeaWRAPPER generateContentIdeas(@V("contentGoal") String contentGoal,
                                                             @V("niche") String niche,
                                                             @V("contentType") String contentType,
                                                             @V("trendingOrEvergreen") String trendingOrEvergreen,
                                                             @V("targetAudience") String targetAudience);

   @UserMessage("Generate 15 engaging Instagram caption for the niche: {{niche}}. The caption should have a {{toneStyle}} tone and end with a call-to-action: {{callToAction}}. Keep the caption short, creative, and Instagram-friendly.in niche this {{niche}}.Generate 15")
   ResponsePOJOInstaCaptionWRAPPER generateCaption(
           @V("niche") String niche,
           @V("toneStyle") String toneStyle,
           @V("callToAction") String callToAction
   );

   @UserMessage("Generate 30 relevant hashtags for the {{niche}} niche using these keywords: {{keywords}}. The hashtags should be {{trendingOrEvergreen}} and optimized for visibility. Generate 30")
   ResponsePOJOInstaHashtagsWRAPPER generateHashtags(
           @V("niche") String niche,
           @V("keywords") List<String> keywords,
           @V("trendingOrEvergreen") String trendingOrEvergreen
   );

   @UserMessage("Suggest 10 engaging content structure for a {{contentType}} post in the {{niche}} niche. The structure should match the {{toneStyle}} tone and ensure high engagement.Generate 10")
   ResponsePOJOInstaDesignAndAestheticWRAPPER generateDesignAndAesthetic(
           @V("niche") String niche,
           @V("toneStyle") String toneStyle,
           @V("contentType") String contentType
   );

   @UserMessage("Generate 10 compelling hook for content with the goal {{contentGoal}}, targeting {{targetAudience}}. The hook should grab attention and drive engagement.Generate 10")
   ResponsePOJOInstaEngagementStrategiesWRAPPER generateEngagementStrategies(
           @V("contentGoal") String contentGoal,
           @V("targetAudience") String targetAudience
   );

   @UserMessage("Suggest best time to post on instagram for {{targetAudience}} in the {{niche}} niche.Take into account when the audience typically engages most, such as during peak hours or specific days of the week.")
   ResponsePOJOInstaBestPostingTimeWRAPPER suggestBestPostTime(
           @V("targetAudience") String targetAudience,
           @V("niche") String niche
   );

}