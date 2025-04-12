package com.MAYA.MAYA.Service.contentServices;

import com.MAYA.MAYA.DTO.tiktok.EngagementPojo.ResponsePOJOTTEngagementWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.hashtagPojo.ResponsePOJOTTHashtagsWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.hooksAndCaptionsPojo.ResponsePOJOTTHooksAndCaptionsWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.musicAndEffectPojo.ResponsePOJOTTMusicAndEffetWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.postingTimePojo.ResponsePOJOTTPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.videoIdeaPojo.ResponsePOJOTTVideoIdeaWRAPPER;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceTikTok {

    @UserMessage("${TTPrompts.generateVideoIdeas}")
    ResponsePOJOTTVideoIdeaWRAPPER generateVideoIdeas(
            @V("niche") String niche,
            @V("videoFormat") String videoFormat,
            @V("videoGoal") String videoGoal,
            @V("targetAudience") String targetAudience,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${TTPrompts.generateHooksAndCaption}")
    ResponsePOJOTTHooksAndCaptionsWRAPPER generateHooksAndCaption(
            @V("toneStyle") String toneStyle,
            @V("callToAction") String callToAction,
            @V("niche") String niche
    );

    @UserMessage("${TTPrompts.suggestHashtag}")
    ResponsePOJOTTHashtagsWRAPPER suggestHashtag(
            @V("videoFormat") String videoFormat,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("niche") String niche,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${TTPrompts.suggestMusicAndEffect}")
    ResponsePOJOTTMusicAndEffetWRAPPER suggestMusicAndEffect(
            @V("toneStyle") String toneStyle,
            @V("videoFormat") String videoFormat,
            @V("niche") String niche
    );

    @UserMessage("${TTPrompts.generateEngagement}")
    ResponsePOJOTTEngagementWRAPPER generateEngagement(
            @V("videoGoal") String videoGoal,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("${TTPrompts.suggestBestPostTime}")
    ResponsePOJOTTPostingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );

}
