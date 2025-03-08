package com.MAYA.MAYA.Service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class instaGramService {

    public String generateContentIdeas(String contentGoal, String niche,String contentType,
     String trendingOrEvergreen,String targetAudience){
        String prompt_one = "Create 5 content ideas for {Content Goal} in the {Niche} niche." +
                " The content type is {Content Type}, targeting {Target Audience}." +
                " Focus on making the content {Trending/Evergreen} and relevant to the audience's current " +
                "interests and challenges.";
        prompt_one = prompt_one.replace("{Content Goal}", contentGoal);
        prompt_one = prompt_one.replace("{Niche}", niche);
        prompt_one = prompt_one.replace("{Content Type}", contentType);
        prompt_one = prompt_one.replace("{Target Audience}", targetAudience);
        prompt_one = prompt_one.replace("{Trending/Evergreen}", trendingOrEvergreen);
        return prompt_one;

    }

    public String generateCaptionWithCTA(String contentIdea,String toneStyle,String callToAction)
    {
        String prompt_two= "Craft a persuasive instagram caption for the content idea: '{Content Idea}'." +
                " Use a {Tone & Style} tone, and ensure the caption encourages action by including the CTA: '{CTA}'." +
                " Focus on being clear and compelling.";
        prompt_two = prompt_two.replace("{Content Idea}", contentIdea);
        prompt_two = prompt_two.replace("{Tone & Style}", toneStyle);
        prompt_two = prompt_two.replace("{CTA}", callToAction);

        return prompt_two;
    }

    public  String suggestHashtags(String niche, List<String> keywords, String trendingOrEvergreen)
    {
        String prompt_three = "Provide 10 instagram hashtags for a {Niche} post about {Keywords}." +
                " Focus on using hashtags that resonate with {Target Audience} and include both trending and niche-specific options." +
                "Optimize for both engagement and discoverability";
        String keywordsString = String.join(", ", keywords);
        prompt_three = prompt_three.replace("{Niche}", niche);
        prompt_three = prompt_three.replace("{Keywords}", keywordsString);



        return prompt_three;
    }

    public String suggestDesignAndAesthetic( String niche,
     String toneStyle,
     String contentType)
    {
        String  prompt_four = "Recommend color schemes, font choices, and visual design elements for an " +
                "instagram {Content Type} post in the {Niche} niche. The design should align with the brand’s " +
                "identity and have a {Tone & Style} style.";
        prompt_four = prompt_four.replace("{Content Type}", contentType);
        prompt_four = prompt_four.replace("{Tone & Style}", toneStyle);
        prompt_four = prompt_four.replace("{Niche}", niche);

        return prompt_four;

    }

    public String generateEngagementStrategies(String contentGoal,String targetAudience)
    {
        String prompt_five = "Provide 3 engagement strategies to boost {Content Goal} for " +
                "instagram content targeted at {Target Audience}. Focus on creating interactive and " +
                "relatable content, including polls, live sessions, and user-generated content." +
                " Aim to increase engagement and build community.";
        prompt_five = prompt_five.replace("{Content Goal}", contentGoal);
        prompt_five = prompt_five.replace("{Target Audience}", targetAudience);

        return prompt_five;
    }

    public String suggestBestPostTime(String targetAudience,String niche)
    {
        String prompt_six = "Suggest the best time to post on instagram for {Target Audience} in the {Niche} niche. " +
                "Take into account when the audience typically engages most, such as during peak hours or specific days " +
                "of the week." ;
        prompt_six = prompt_six.replace("{Niche}", niche);
        prompt_six = prompt_six.replace("{Target Audience}", targetAudience);
        return prompt_six;
    }

}
