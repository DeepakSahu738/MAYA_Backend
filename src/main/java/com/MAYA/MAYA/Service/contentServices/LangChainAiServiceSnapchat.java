package com.MAYA.MAYA.Service.contentServices;

import com.MAYA.MAYA.DTO.snapchat.EngagementFeaturesPojo.ResponsePOJOSCEngagementFeaturesWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.boostingTipsPojo.ResponsePOJOSCBoostingTipsWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.filtersPojo.ResponsePOJOSClensesAndFiltersWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.postingTimePojo.ResponsePOJOSCPOstingTimeWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.storyIdeaPojo.ResponsePOJOSCStoryIdeaWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.textOverlaysPojo.ResponsePOJOSCTextOverlaysWRAPPER;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceSnapchat {

    @UserMessage("${SCPrompts.generateStoryIdeas}")
    ResponsePOJOSCStoryIdeaWRAPPER generateStoryIdeas(
            @V("snapGoal") String snapGoal,
            @V("niche") String niche,
            @V("storyType") String storyType,
            @V("toneStyle") String toneStyle,
            @V("callToAction") String callToAction,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${SCPrompts.generateTextOverlays}")
    ResponsePOJOSCTextOverlaysWRAPPER generateTextOverlays(
            @V("storyType") String storyType,
            @V("toneStyle") String toneStyle,
            @V("niche") String niche,
            @V("callToAction") String callToAction,
            @V("stickersAndFilters") List<String> stickersAndFilters,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${SCPrompts.suggestTrendingLensesAndFilters}")
    ResponsePOJOSClensesAndFiltersWRAPPER suggestTrendingLensesAndFilters(
            @V("niche") String niche,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("storyType") String storyType,
            @V("targetAudience") String targetAudience,
            @V("stickersAndFilters") List<String> stickersAndFilters
    );

    @UserMessage("${SCPrompts.suggestEngagementFeatures}")
    ResponsePOJOSCEngagementFeaturesWRAPPER suggestEngagementFeatures(
            @V("toneStyle") String toneStyle,
            @V("storyType") String storyType,
            @V("niche") String niche
    );

    @UserMessage("${SCPrompts.generateBoostingTips}")
    ResponsePOJOSCBoostingTipsWRAPPER generateBoostingTips(); // No params — no @V needed

    @UserMessage("${SCPrompts.suggestBestPostTime}")
    ResponsePOJOSCPOstingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience,
            @V("storyType") String storyType
    );
}
