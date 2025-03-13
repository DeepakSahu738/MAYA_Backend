package com.MAYA.MAYA.Service;

import dev.langchain4j.chain.Chain;
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

        return chatModel.generate(result.text());

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

        return chatModel.generate(result.text());
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

        return chatModel.generate(result.text());
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

        return chatModel.generate(result.text());
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

        return chatModel.generate(result.text());
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

        return chatModel.generate(result.text());
    }


    public String generateFullContent(String contentGoal, String niche, String contentType,
                                      String trendingOrEvergreen, String targetAudience,
                                      String toneStyle, String callToAction) {

        // Step 1: Create Content Idea Prompt
        PromptTemplate contentIdeaTemplate = PromptTemplate.from(
                "Create 5 content ideas for '{{contentGoal}}' in the '{{niche}}' niche." +
                        " The content type is '{{contentType}}', targeting '{{targetAudience}}'." +
                        " Focus on making the content '{{trendingOrEvergreen}}' and relevant to the audience's current " +
                        "interests and challenges."
        );

        // Step 2: Create Caption Prompt (Using output from Step 1)
        PromptTemplate captionTemplate = PromptTemplate.from(
                "Craft a persuasive instagram caption for the content idea: '{{contentIdea}}'." +
                        " Use a '{{toneStyle}}' tone, and ensure the caption encourages action by including the CTA: '{{callToAction}}'." +
                        " Focus on being clear and compelling."
        );

        // Step 3: Create Hashtags Prompt (Using output from Step 1)
        PromptTemplate hashtagsTemplate = PromptTemplate.from(
                "Provide 10 instagram hashtags for a '{{niche}}' post about '{{keywords}]'." +
                        " Focus on using hashtags that resonate with '{{trendingOrEvergreen}}' and include both trending and niche-specific options." +
                        "Optimize for both engagement and discoverability"
        );
        // Step 4: Create Design And Aesthetic Prompt (Using output from Step 1)
        PromptTemplate designAndAestheticTemplate = PromptTemplate.from(
                "Recommend color schemes, font choices, and visual design elements for an " +
                        "instagram '{{contentType}}' post in the '{{niche}}' niche. The design should align with the brand’s " +
                        "identity and have a '{{toneStyle}}' style."
        );

        // Step 5: Create Hashtags Prompt (Using output from Step 1)
        PromptTemplate engagementStrategiesTemplate = PromptTemplate.from(
                "Provide 3 engagement strategies to boost '{{contentGoal}}' for " +
                        "instagram content targeted at '{{targetAudience}}'. Focus on creating interactive and " +
                        "relatable content, including polls, live sessions, and user-generated content." +
                        " Aim to increase engagement and build community."
        );

        // Step 6: Create BestPostTime Prompt (Using output from Step 1)
        PromptTemplate bestPostTimeTemplate = PromptTemplate.from(
                "Suggest the best time to post on instagram for '{{targetAudience}}' in the '{{niche}}' niche. " +
                        "Take into account when the audience typically engages most, such as during peak hours or specific days " +
                        "of the week."
        );





        // Create the chain
        Chain<String,String> chain = contentIdeaTemplate
                .then(input -> {
                    // Generate content idea
                    String contentIdea = chatModel.generate(input);
                    System.out.println("Generated Content Idea: " + contentIdea);

                    // Pass output to caption and hashtags prompt
                    String caption = chatModel.generate(captionTemplate.apply(Map.of(
                            "contentIdea", contentIdea,
                            "toneStyle", toneStyle,
                            "callToAction", callToAction
                    )));

                    String hashtags = chatModel.generate(hashtagsTemplate.apply(Map.of(
                            "niche", niche,
                            "contentIdea", contentIdea
                    )));

                    return "Content Idea:\n" + contentIdea +
                            "\n\nCaption:\n" + caption +
                            "\n\nHashtags:\n" + hashtags;
                });

        // Run the chain
        String result = chain.execute(Map.of(
                "contentGoal", contentGoal,
                "niche", niche,
                "contentType", contentType,
                "trendingOrEvergreen", trendingOrEvergreen,
                "targetAudience", targetAudience
        ));

        return result;
    }

}
