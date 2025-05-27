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

    @UserMessage("Generate five highly engaging, platform-native Snapchat story ideas designed to achieve {{snapGoal}} within the {{niche}} space.Ensure ideas fit {{storyType}} and resonate with {{targetAudience}}, using a {{toneStyle}} approach.Stories should be structured with a strong hook, middle intrigue, and CTA like {{callToAction}}.Examples: If {{trendingOrEvergreen}} is trending,suggest current viral formats. If evergreen, suggest timeless, repeatable formats.")
    ResponsePOJOSCStoryIdeaWRAPPER generateStoryIdeas(
            @V("snapGoal") String snapGoal,
            @V("niche") String niche,
            @V("storyType") String storyType,
            @V("toneStyle") String toneStyle,
            @V("targetAudience") String targetAudience,
            @V("callToAction") String callToAction,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("Craft five dynamic, attention-grabbing overlays that perfectly match {{storyType}} and {{toneStyle}}.Include keywords related to {{niche}} and elements that encourage {{callToAction}}.Overlays should be short, punchy, and FOMO-driven (e.g., MUST-WATCH! {{stickersAndFilters}} or {{targetAudience}}, This is for YOU!).")
    ResponsePOJOSCTextOverlaysWRAPPER generateTextOverlays(
            @V("storyType") String storyType,
            @V("toneStyle") String toneStyle,
            @V("niche") String niche,
            @V("callToAction") String callToAction,
            @V("stickersAndFilters") List<String> stickersAndFilters,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("Identify AR filters and lenses that complement {{niche}} and align with {{trendingOrEvergreen}} trends.Suggest interactive stickers that match {{storyType}}, keeping engagement high for {{targetAudience}}.If {{stickersAndFilters}} includes branding, propose custom filter ideas (e.g., Your Brand Name overlay on a popular effect).")
    ResponsePOJOSClensesAndFiltersWRAPPER suggestTrendingLensesAndFilters(
            @V("niche") String niche,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("storyType") String storyType,
            @V("targetAudience") String targetAudience,
            @V("stickersAndFilters") List<String> stickersAndFilters
    );

    @UserMessage("Recommend Snap-native engagement tools that fit {{toneStyle}}, {{storyType}}, and {{niche}}.Include polls, quizzes, countdown timers, and reply stickers that encourage audience participation.Example: If {{storyType}} is Behind-the-Scenes, suggest a Swipe Up for BTS challenge.")
    ResponsePOJOSCEngagementFeaturesWRAPPER suggestEngagementFeatures(
            @V("toneStyle") String toneStyle,
            @V("storyType") String storyType,
            @V("niche") String niche
    );

    @UserMessage("Provide strategies specific to {{snapGoal}}, such as leveraging daily streaks, influencer takeovers, or interactive Q&As.Recommend posting sequences (e.g., Start with a teaser, then a reveal, followed by a poll).Suggest best-performing themes in {{niche}} (e.g., Fitness = Before/After results, Fashion = Try-On Haul).")
    ResponsePOJOSCBoostingTipsWRAPPER generateBoostingTips(
            @V("snapGoal") String snapGoal,
            @V("niche") String niche
    );

    @UserMessage("Identify optimal posting times based on {{targetAudience}} behavior (e.g., Teens engage most between 7-10 PM).Adjust recommendations based on {{storyType}} (e.g., Flashbacks perform best in the evening).")
    ResponsePOJOSCPOstingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience,
            @V("storyType") String storyType
    );
}
