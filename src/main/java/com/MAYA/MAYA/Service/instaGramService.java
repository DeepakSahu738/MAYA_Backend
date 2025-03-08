package com.MAYA.MAYA.Service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class instaGramService {

    public String createPrompt_ONE(String contentGoal, String niche,String contentType,
     String trendingOrEvergreen,String targetAudience){
        String prompt_one = "Generate 3 unique Instagram content ideas for a {Content Goal} " +
                "in the {Niche} niche. The content type is {Content Type}, targeting {Target Audience}." +
                " The content should be {Trending/Evergreen} and align with the audience's interests.";
        prompt_one = prompt_one.replace("{Content Goal}", contentGoal);
        prompt_one = prompt_one.replace("{Niche}", niche);
        prompt_one = prompt_one.replace("{Content Type}", contentType);
        prompt_one = prompt_one.replace("{Target Audience}", targetAudience);
        prompt_one = prompt_one.replace("{Trending/Evergreen}", trendingOrEvergreen);
        return prompt_one;

    }

    public String createPrompt_TWO(String contentIdea,String toneStyle,String callToAction)
    {
        String prompt_two= "Write an engaging Instagram caption for the content idea: '{Content Idea}'." +
                " The caption should have a {Tone & Style} " +
                "tone and end with a call-to-action: '{CTA}'." +
                " Keep the caption short, creative, and Instagram-friendly.";
        prompt_two = prompt_two.replace("{Content Idea}", contentIdea);
        prompt_two = prompt_two.replace("{Tone & Style}", toneStyle);
        prompt_two = prompt_two.replace("{CTA}", callToAction);

        return prompt_two;
    }

    public  String createPrompt_THREE(String niche, List<String> keywords, String trendingOrEvergreen)
    {
        String prompt_three = "Suggest 10 Instagram hashtags for a {Niche} post about " +
                "{Keywords}. Include a mix of trending and niche-specific hashtags.";
        String keywordsString = String.join(", ", keywords);
        prompt_three = prompt_three.replace("{Niche}", niche);
        prompt_three = prompt_three.replace("{Keywords}", keywordsString);



        return prompt_three;
    }

    public String createPrompt_FOUR( String niche,
     String toneStyle,
     String contentType)
    {
        String  prompt_four = "Suggest color palettes, font styles," +
                " and visual elements for an Instagram {Content Type} post in the {Niche}" +
                " niche with a {Tone & Style} style.";
        prompt_four = prompt_four.replace("{Content Type}", contentType);
        prompt_four = prompt_four.replace("{Tone & Style}", toneStyle);
        prompt_four = prompt_four.replace("{Niche}", niche);

        return prompt_four;

    }

    public String createPrompt_FIVE(String contentGoal,String targetAudience)
    {
        String prompt_five = "Suggest 3 engagement strategies to maximize " +
                "{Content Goal} for Instagram content targeted at {Target Audience}." +
                " Include interactive ideas like stories, giveaways, and Q&A.";
        prompt_five = prompt_five.replace("{Content Goal}", contentGoal);
        prompt_five = prompt_five.replace("{Target Audience}", targetAudience);

        return prompt_five;
    }

    public String createPrompt_SIX(String targetAudience,String niche)
    {
        String prompt_six = "Based on common Instagram engagement patterns," +
                " recommend the best posting time for {Target Audience} in the {Niche} niche." ;
        prompt_six = prompt_six.replace("{Niche}", niche);
        prompt_six = prompt_six.replace("{Target Audience}", targetAudience);
        return "";
    }

}
