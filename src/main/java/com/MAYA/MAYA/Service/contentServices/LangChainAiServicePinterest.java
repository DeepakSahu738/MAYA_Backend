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
    @UserMessage("Generate high-performing pin topics designed to achieve {{pinGoal}}, whether it?s driving {{pinGoal}} through visually appealing content.Focus on trending topics or long-lasting inspiration depending on {{trendingOrEvergreen}}.Align content format with {{pinType}}, ensuring it matches what works best on Pinterest (e.g., infographics for tutorials, video pins for step-by-step guides, single images for aesthetics).Ensure that the ideas resonate with {{targetAudience}}, considering their demographics and search behavior.")
    ResponsePOJOPPinIdeaWRAPPER generatePinIdeas(
            @V("pinGoal") String pinGoal,
            @V("trendingOrEvergreen") String trendingOrEvergreen,
            @V("pinType") String pinType,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("Craft catchy, keyword-rich titles that align with {{searchKeywords}}, ensuring Pinterest SEO optimization for better reach. Write concise, compelling descriptions that provide context while naturally integrating {{searchKeywords}}.Structure descriptions to encourage engagement, incorporating action words and a strong {{callToAction}}.")
    ResponsePOJOPTitleAndDescriptionWRAPPER generateOptimizedTitleAndDescription(
            @V("searchKeywords") List<String> searchKeywords,
            @V("callToAction") String callToAction
    );

    @UserMessage("Identify high-ranking keywords for {{niche}} and {{pinGoal}} that align with Pinterest's search algorithm.Suggest a mix of short-tail and long-tail keywords to balance broad discovery and niche targeting.Recommend relevant hashtags & keyword variations to maximize visibility in Pinterest Smart Feed.")
    ResponsePOJOPHashtagWRAPPER suggestHashtag(
            @V("niche") String niche,
            @V("pinGoal") String pinGoal
    );

    @UserMessage("Provide Aesthetic and design recommendations based on {{toneStyle}} to ensure the pin is visually aligned with Pinterest trends (e.g., minimalist for lifestyle, vibrant for DIY).Suggest optimal color schemes in the same line, fonts, and overlays that enhance engagement and repin potential.Recommend layout strategies tailored to {{pinType}}, ensuring a scroll-stopping design.")
    ResponsePOJOPAestheticAndDesignWRAPPER suggestAestheticAndDesignTips(
            @V("toneStyle") String toneStyle,
            @V("pinType") String pinType
    );

    @UserMessage("Suggest group boards & repin strategies to amplify visibility and engagement within {{niche}}.Recommend Rich Pins, Idea Pins, and carousel posts for boosting reach and providing more context to the content.Develop engagement-focused captions with a clear {{callToAction}} (e.g., Save this for later! Click to read more!).")
    ResponsePOJOPEngagementWRAPPER generateEngagement(
            @V("niche") String niche,
            @V("callToAction") String callToAction
    );

    @UserMessage("Identify optimal posting times based on {{targetAudience}} and their scrolling habits.Suggest best days of the week to post for maximum repins and traffic. Recommend scheduling strategies to ensure consistent visibility on Pinterest?s Smart Feed.")
    ResponsePOJOPPostingTimeWRAPPER suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );
}
