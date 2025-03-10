package com.MAYA.MAYA.Service;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class instaGramService {

    public final OpenAiChatModel chatModel;

    public instaGramService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateContentIdeas(String contentGoal, String niche,String contentType,
     String trendingOrEvergreen,String targetAudience){
        PromptTemplate template = PromptTemplate.from(
                "Create 5 content ideas for '{{contentGoal}}' in the '{{niche}}' niche." +
                        " The content type is '{{contentType}}', targeting '{{targetAudience}}'." +
                        " Focus on making the content '{{trendingOrEvergreen}}' and relevant to the audience's current " +
                        "interests and challenges."
        );
        Map<String, Object> variables = new HashMap<>();
        variables.put("contentGoal", contentGoal);
        variables.put("niche", niche);
        variables.put("contentType", contentType);
        variables.put("targetAudience", targetAudience);
        variables.put("trendingOrEvergreen", trendingOrEvergreen);

        Prompt result = template.apply(variables);

        return chatModel.generate(String.valueOf(result));

    }

    public String generateCaptionWithCTA(String contentIdea,String toneStyle,String callToAction)
    {
        PromptTemplate template = PromptTemplate.from(
                "Craft a persuasive instagram caption for the content idea: '{{contentIdea}}'." +
                        " Use a '{{toneStyle}}' tone, and ensure the caption encourages action by including the CTA: '{{callToAction}}'." +
                        " Focus on being clear and compelling."
        );
        Map<String, Object> variables = new HashMap<>();
        variables.put("contentIdea", contentIdea);
        variables.put("toneStyle", toneStyle);
        variables.put("callToAction", callToAction);

        Prompt result = template.apply(variables);

        return chatModel.generate(String.valueOf(result));
    }

    public  String suggestHashtags(String niche, List<String> keywords, String trendingOrEvergreen)
    {
        PromptTemplate template = PromptTemplate.from(
                "Provide 10 instagram hashtags for a '{{niche}}' post about '{{keywords}]'." +
                        " Focus on using hashtags that resonate with '{{trendingOrEvergreen}}' and include both trending and niche-specific options." +
                        "Optimize for both engagement and discoverability"
        );
        String keywordsString = String.join(", ", keywords);
        Map<String, Object> variables = new HashMap<>();
        variables.put("niche", niche);
        variables.put("keywords", keywordsString);
        variables.put("trendingOrEvergreen", trendingOrEvergreen);


        Prompt result = template.apply(variables);

        return chatModel.generate(String.valueOf(result));
    }

    public String suggestDesignAndAesthetic( String niche,
     String toneStyle,
     String contentType)
    {
        PromptTemplate template = PromptTemplate.from(
                "Recommend color schemes, font choices, and visual design elements for an " +
                        "instagram '{{contentType}}' post in the '{{niche}}' niche. The design should align with the brand’s " +
                        "identity and have a '{{toneStyle}}' style."
        );
        Map<String, Object> variables = new HashMap<>();
        variables.put("toneStyle", toneStyle);
        variables.put("niche", niche);
        variables.put("contentType", contentType);

        Prompt result = template.apply(variables);

        return chatModel.generate(String.valueOf(result));

    }

    public String generateEngagementStrategies(String contentGoal,String targetAudience)
    {

        PromptTemplate template = PromptTemplate.from(
                "Provide 3 engagement strategies to boost '{{contentGoal}}' for " +
                        "instagram content targeted at '{{targetAudience}}'. Focus on creating interactive and " +
                        "relatable content, including polls, live sessions, and user-generated content." +
                        " Aim to increase engagement and build community."
        );
        Map<String, Object> variables = new HashMap<>();
        variables.put("contentGoal", contentGoal);
        variables.put("targetAudience", targetAudience);

        Prompt result = template.apply(variables);

        return chatModel.generate(String.valueOf(result));
    }

    public String suggestBestPostTime(String targetAudience,String niche)
    {
        PromptTemplate template = PromptTemplate.from(
                "Suggest the best time to post on instagram for '{{targetAudience}}' in the '{{niche}}' niche. " +
                        "Take into account when the audience typically engages most, such as during peak hours or specific days " +
                        "of the week."
        );
        Map<String, Object> variables = new HashMap<>();
        variables.put("niche", niche);
        variables.put("targetAudience", targetAudience);

        Prompt result = template.apply(variables);

        return chatModel.generate(String.valueOf(result));
    }

}
