package com.MAYA.MAYA.Service.contentServices;

import com.MAYA.MAYA.DTO.youtube.engagementPojo.ResponsePOJOYTEngagementWRAPPER;
import com.MAYA.MAYA.DTO.youtube.hashtagPojo.ResponsePOJOYTHashtagWRAPPER;
import com.MAYA.MAYA.DTO.youtube.postingTime.ResponsePOJOYTPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.youtube.seoPojo.ResponsePOJOYTSEOWRAPPER;
import com.MAYA.MAYA.DTO.youtube.thumbnailAndBranding.ResponsePOJOYTThumbnailAndBrandingWRAPPER;
import com.MAYA.MAYA.DTO.youtube.videoIdeaPojo.ResponsePOJOYTVideoIdeaWRAPPER;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceYoutube {

    @UserMessage("${YTPrompts.generateVideoIdeas}")
    ResponsePOJOYTVideoIdeaWRAPPER generateVideoIdeas(
            @V("videoGoal") String videoGoal,
            @V("niche") String niche,
            @V("videoType") String videoType,
            @V("targetAudience") String targetAudience,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${YTPrompts.generateSEO}")
    ResponsePOJOYTSEOWRAPPER generateSEO(
            @V("keywordsAndSeoTags") List<String> keywordsAndSeoTags,
            @V("callToAction") String callToAction
    );

    @UserMessage("${YTPrompts.suggestHashtag}")
    ResponsePOJOYTHashtagWRAPPER suggestHashtag(
            @V("keywordsAndSeoTags") List<String> keywordsAndSeoTags,
            @V("niche") String niche,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("${YTPrompts.suggestThumbnailAndBranding}")
    ResponsePOJOYTThumbnailAndBrandingWRAPPER suggestThumbnailAndBranding(
            @V("toneStyle") String toneStyle,
            @V("niche") String niche,
            @V("videoGoal") String videoGoal
    );

    @UserMessage("${YTPrompts.generateEngagement}")
    ResponsePOJOYTEngagementWRAPPER generateEngagement(
            @V("targetAudience") String targetAudience,
            @V("niche") String niche
    );

    @UserMessage("${YTPrompts.suggestBestPostTime}")
    ResponsePOJOYTPostingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );

}
