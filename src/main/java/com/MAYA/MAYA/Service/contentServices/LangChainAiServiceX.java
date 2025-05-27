package com.MAYA.MAYA.Service.contentServices;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface LangChainAiServiceX {

    @UserMessage("Generate highly engaging tweet ideas tailored for {{tweetGoal}}, focusing on {{niche}} and resonating with {{targetAudience}}. Create tweet structures matching {{tweetType}}, ensuring the best format for visibility (e.g., thought-provoking one-liners for viral reach, deep threads for engagement, polls for interaction).Align ideas with {{trendingOrEvergreen}}, ensuring either viral momentum or long-lasting impact.")
    List<String> generateTweetIdeas(
            @V("tweetGoal") String tweetGoal,
            @V("niche") String niche,
            @V("targetAudience") String targetAudience,
            @V("tweetType") String tweetType,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("Craft scroll-stopping tweet copy that matches {{toneStyle}}, ensuring it captures attention instantly. Incorporate {{hashtagsAndMentions}} naturally to enhance visibility while keeping the text concise & engaging.Conclude with a strong {{callToAction}}, prompting {{targetAudience}} to interact (e.g., Retweet if you agree, Reply with your thoughts,Follow for more insights).")
    String generateOptimizedTweetCopy(
            @V("toneStyle") String toneStyle,
            @V("hashtagsAndMentions") List<String> hashtagsAndMentions,
            @V("callToAction") String callToAction,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("Select 10 high-impact hashtags based on {{hashtagsAndMentions}}, ensuring a mix of trending, niche-specific, and evergreen tags.Identify relevant accounts to tag (influencers, brands, thought leaders) to amplify reach & encourage engagement.Recommend trending hashtags & topics that align with {{niche}} and {{trendingOrEvergreen}}, optimizing for maximum visibility on the platform.")
    String suggestHashtag(
            @V("hashtagsAndMentions") List<String> hashtagsAndMentions,
            @V("niche") String niche,
            @V("trendingOrEvergreen") String trendingOrEvergreen
    );

    @UserMessage("Suggest highly engaging visuals (memes, GIFs, infographics, screenshots) that fit {{tweetType}} and {{toneStyle}}.Recommend GIFs, reaction images, or short video clips that increase tweet retention and match {{niche}} & {{targetAudience}} preferences.")
    String suggestVisualAndGIFSuggestions(
            @V("tweetType") String tweetType,
            @V("toneStyle") String toneStyle,
            @V("niche") String niche,
            @V("targetAudience") String targetAudience
    );

    @UserMessage("Develop reply strategies to spark conversation with {{targetAudience}}, such as thought-provoking questions or challenge-based replies.Recommend Twitter Spaces topics and formats to position the tweet for discussion & networking.Suggest poll questions that align with {{niche}} and encourage user participation.")
    String generateEngagement(
            @V("targetAudience") String targetAudience,
            @V("niche") String niche
    );

    @UserMessage("Identify the ideal tweet schedule based on {{targetAudience}} habits, ensuring maximum visibility.Optimize posting times for real-time interactions (e.g., peak hours for discussions vs. late-night viral tweets).Recommend tweet scheduling strategies for different time zones to ensure broader reach.")
    String suggestBestPostTime(
            @V("targetAudience") String targetAudience
    );

}
