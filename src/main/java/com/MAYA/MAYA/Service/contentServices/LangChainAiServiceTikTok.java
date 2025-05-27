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

    @UserMessage("Suggest five viral TikTok video ideas tailored for {{niche}}, formatted as {{videoFormat}}.Ensure ideas align with {{videoGoal}} (e.g., if growth is the goal, suggest collabs & duets).Adapt to {{targetAudience}} trends & behavior (e.g., short-form challenges for Gen Z).If {{trendingOrEvergreen}} is trending, recommend high-impact challenges.")
    ResponsePOJOTTVideoIdeaWRAPPER generateVideoIdeas(
            @V("niche") String niche,
            @V("videoFormat") String videoFormat,
            @V("videoGoal") String videoGoal,
            @V("targetAudience") String targetAudience,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("Create 5 viral hook and captions lines that instantly grab attention in the first 3 seconds .Format should match {{toneStyle}} (e.g., Fun: You won?t believe what happens next! / Aesthetic: POV: You just stepped into your dream life ).Suggest 3 caption variations that encourage comment stacking (e.g., ask polarizing or relatable questions, use Finish the sentence techniques).Ensure the caption includes a strong CTA, like {{callToAction}}, e.g., Tag a friend who needs this! or Follow for more {{niche}} tips!")
    ResponsePOJOTTHooksAndCaptionsWRAPPER generateHooksAndCaption(
            @V("toneStyle") String toneStyle,
            @V("callToAction") String callToAction,
            @V("niche") String niche
    );

    @UserMessage("Provide 10 TikTok-optimized hashtags:5 trending hashtags specific to {{videoFormat}} & {{trendingOrEvergreen}} (e.g., #DuetThis, #POV, #FYP).5 niche-focused hashtags targeting {{niche}} and {{targetAudience}} (e.g., #TechTok for tech, #FitnessForYou for fitness).Ensure hashtags follow TikTok discovery algorithm trends (broad reach + niche targeting balance).")
    ResponsePOJOTTHashtagsWRAPPER suggestHashtag(
            @V("videoFormat") String videoFormat,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("niche") String niche,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("Provide 3 trending sounds and effects that enhance {{toneStyle}} and {{videoFormat}}.Suggest 3 effect recommendations that fit {{toneStyle}} and elevate content appeal.")
    ResponsePOJOTTMusicAndEffetWRAPPER suggestMusicAndEffect(
            @V("toneStyle") String toneStyle,
            @V("videoFormat") String videoFormat,
            @V("niche") String niche
    );

    @UserMessage("Outline 3 TikTok-specific engagement tactics that align with {{videoGoal}} and {{targetAudience}}.Suggest interactive features such as polls, duets, or stitch-friendly content.")
    ResponsePOJOTTEngagementWRAPPER generateEngagement(
            @V("videoGoal") String videoGoal,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("Recommend 2-3 optimal posting times based on {{targetAudience}} engagement patterns.")
    ResponsePOJOTTPostingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );

}
