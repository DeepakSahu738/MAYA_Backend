package com.MAYA.MAYA.Service.contentServices;

import com.MAYA.MAYA.DTO.facebook.adAndBoostingPojo.ResponsePOJOFBadAndBoostingWRAPPER;
import com.MAYA.MAYA.DTO.facebook.engagementFeaturePojo.ResponsePOJOFBengagementFeatureWRAPPER;
import com.MAYA.MAYA.DTO.facebook.hashtagsPojo.ResponsePOJOFBhashtagsWRAPPER;
import com.MAYA.MAYA.DTO.facebook.headlinesAndDesPojo.ResponsePOJOFBheadlinesAndDes;
import com.MAYA.MAYA.DTO.facebook.headlinesAndDesPojo.ResponsePOJOFBheadlinesAndDesWRAPPER;
import com.MAYA.MAYA.DTO.facebook.postIdeaPojo.ResponsePOJOFBPostIdeaWRAPPER;
import com.MAYA.MAYA.DTO.facebook.postingTimePojo.ResponsePOJOFBPostingTimeWRAPPER;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceFacebook {

    @UserMessage("Generate five post ideas optimized for high engagement based on {{postGoal}} and {{niche}}. Ensure each idea includes a strong hook to stop scrolling.")
    ResponsePOJOFBPostIdeaWRAPPER generatePostIdeas(
            @V("postGoal") String postGoal,
            @V("niche") String niche
    );

    @UserMessage("Write an attention-grabbing first line and a compelling description that suits {{postType}}, aligns with {{toneStyle}}, and sparks conversation within {{targetAudience}}.")
    ResponsePOJOFBheadlinesAndDesWRAPPER generateHeadlinesAndDes(
            @V("postType") String postType,
            @V("toneStyle") String toneStyle,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("Provide trending, niche-specific hashtags related to {{topicsAndKeywords}}. Suggest high-engagement pages, influencers, or groups to tag.")
    ResponsePOJOFBhashtagsWRAPPER suggestHashtags(
            @V("topicsAndKeywords") List<String> topicsAndKeywords
    );

    @UserMessage("Recommend engagement-driving elements (polls, quizzes, challenges) suited for {{postType}} to boost interaction rate.")
    ResponsePOJOFBengagementFeatureWRAPPER suggestEngagementFeatures(
            @V("postType") String postType
    );

    @UserMessage("Suggest the best ad format (Carousel, Video, Story, Static Image) and hacks to improve paid reach, including budget allocation and A/B testing.")
    ResponsePOJOFBadAndBoostingWRAPPER generateBoostingTips(); // No parameters, so no @V needed

    @UserMessage("Recommend ideal posting days & time slots for {{targetAudience}} based on recent engagement trends.")
    ResponsePOJOFBPostingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );
}
