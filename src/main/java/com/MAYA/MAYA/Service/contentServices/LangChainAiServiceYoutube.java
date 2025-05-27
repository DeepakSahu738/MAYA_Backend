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

    @UserMessage("Provide 5 video ideas optimized for {{videoGoal}} in the {{niche}} category.Structure each idea to fit the {{videoType}}, maximizing watch time and audience retention.Ensure the content style resonates with {{targetAudience}} preferences. Align the themes with {{trendingOrEvergreen}} trends to boost longevity.")
    ResponsePOJOYTVideoIdeaWRAPPER generateVideoIdeas(
            @V("videoGoal") String videoGoal,
            @V("niche") String niche,
            @V("videoType") String videoType,
            @V("targetAudience") String targetAudience,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("Generate 3 compelling video titles using {{keywordsAndSeoTags}} for high-ranking search results.Create 2 detailed video descriptions, including keyword-rich first sentences to increase click-through rates.Ensure descriptions naturally incorporate {{callToAction}} for audience engagement.")
    ResponsePOJOYTSEOWRAPPER generateSEO(
            @V("keywordsAndSeoTags") List<String> keywordsAndSeoTags,
            @V("callToAction") String callToAction
    );

    @UserMessage("Generate 10 relevant hashtags that align with {{keywordsAndSeoTags}}, ensuring a mix of high-volume and niche-specific tags.Suggest video tags that match {{keywordsAndSeoTags}} to improve discoverability under related videos.Optimize hashtags for {{niche}} and {{trendingOrEvergreen}} content to increase reach across search, suggested, and trending sections.Ensure the hashtags naturally complement the video title & description, reinforcing searchability.")
    ResponsePOJOYTHashtagWRAPPER suggestHashtag(
            @V("keywordsAndSeoTags") List<String> keywordsAndSeoTags,
            @V("niche") String niche,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("Recommend 3 thumbnail design styles tailored to {{toneStyle}} and {{niche}}.Suggest best color schemes, fonts, and visual cues that boost click-through rates.Incorporate elements that align with {{videoGoal}}, increasing brand consistency and engagement. keep the output in the object with only one node and in the same line")
    ResponsePOJOYTThumbnailAndBrandingWRAPPER suggestThumbnailAndBranding(
            @V("toneStyle") String toneStyle,
            @V("niche") String niche,
            @V("videoGoal") String videoGoal
    );

    @UserMessage("Outline 3 YouTube engagement tactics based on {{targetAudience}} behavior. Suggest interactive features like Polls, Community Posts, Pinned Comments, and Shorts strategy to keep the audience engaged .Provide 2 collaboration ideas (cross-promotions or guest appearances) that fit the {{niche}}.")
    ResponsePOJOYTEngagementWRAPPER generateEngagement(
            @V("targetAudience") String targetAudience,
            @V("niche") String niche
    );

    @UserMessage("Recommend 2-3 optimal upload times based on {{targetAudience}} viewing habits.Suggest adjustments for weekdays vs. weekends to maximize reach and watch time.")
    ResponsePOJOYTPostingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );

}
