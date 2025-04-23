package com.MAYA.MAYA.Service.contentServices;

import com.MAYA.MAYA.DTO.pinterest.aestheticAndDesign.ResponsePOJOPAestheticAndDesignWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.engagement.ResponsePOJOPEngagementWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.hashtag.ResponsePOJOPHashtagWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.pinIdea.ResponsePOJOPPinIdeaWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.postingTime.ResponsePOJOPPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.titleAndDescription.ResponsePOJOPTitleAndDescriptionWRAPPER;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServicePinterest {
    @UserMessage("${PinPrompts.generatePinIdeas}")
    ResponsePOJOPPinIdeaWRAPPER generatePinIdeas(
            @V("pinGoal") String pinGoal,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("pinType") String pinType,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${PinPrompts.generateOptimizedTitleAndDescription}")
    ResponsePOJOPTitleAndDescriptionWRAPPER generateOptimizedTitleAndDescription(
            @V("searchKeywords") List<String> searchKeywords,
            @V("callToAction") String callToAction
    );

    @UserMessage("${PinPrompts.suggestHashtag}")
    ResponsePOJOPHashtagWRAPPER suggestHashtag(
            @V("niche") String niche,
            @V("pinGoal") String pinGoal
    );

    @UserMessage("${PinPrompts.suggestAestheticAndDesignTips}")
    ResponsePOJOPAestheticAndDesignWRAPPER suggestAestheticAndDesignTips(
            @V("toneStyle") String toneStyle,
            @V("pinType") String pinType
    );

    @UserMessage("${PinPrompts.generateEngagement}")
    ResponsePOJOPEngagementWRAPPER generateEngagement(
            @V("niche") String niche,
            @V("callToAction") String callToAction
    );

    @UserMessage("${PinPrompts.suggestBestPostTime}")
    ResponsePOJOPPostingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );
}
