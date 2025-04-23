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

    @UserMessage("${FBPrompts.generatePostIdeas}")
    ResponsePOJOFBPostIdeaWRAPPER generatePostIdeas(
            @V("postGoal") String postGoal,
            @V("niche") String niche
    );

    @UserMessage("${FBPrompts.generateHeadlinesAndDes}")
    ResponsePOJOFBheadlinesAndDesWRAPPER generateHeadlinesAndDes(
            @V("postType") String postType,
            @V("toneStyle") String toneStyle,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${FBPrompts.suggestHashtags}")
    ResponsePOJOFBhashtagsWRAPPER suggestHashtags(
            @V("topicsAndKeywords") List<String> topicsAndKeywords
    );

    @UserMessage("${FBPrompts.suggestEngagementFeatures}")
    ResponsePOJOFBengagementFeatureWRAPPER suggestEngagementFeatures(
            @V("postType") String postType
    );

    @UserMessage("${FBPrompts.generateBoostingTips}")
    ResponsePOJOFBadAndBoostingWRAPPER generateBoostingTips(); // No parameters, so no @V needed

    @UserMessage("${FBPrompts.suggestBestPostTime}")
    ResponsePOJOFBPostingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );
}
