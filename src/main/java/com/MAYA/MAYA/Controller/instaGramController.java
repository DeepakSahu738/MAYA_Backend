package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.instagram.*;
import com.MAYA.MAYA.Service.instaGramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
@CrossOrigin(origins = "http://localhost:9090")
public class instaGramController {

    @Autowired
    private instaGramService instaGramService;
    @PostMapping("/insta_one")
    public Map<String, String> generateContentIdea(@RequestBody ContentIdeaDTO request) {

        // Simulated response (Later, we will integrate LangChain AI here)
        String generateCoreIdea = instaGramService.generateContentIdeas(request.getContentGoal(),request.getNiche(),request.getContentType()
        ,request.getTrendingOrEvergreen(),request.getTargetAudience());

        // Returning a JSON response  not using the instaService currently will integrate when we integrate Langchain AI
        Map<String, String> response = new HashMap<>();
        response.put("contentIdea", generateCoreIdea);
        return response;
    }

    @PostMapping("/insta_two")
    public Map<String, String> generateCaption(@RequestBody CaptionDTO request) {

        // Simulated response (Later, we will integrate LangChain AI here)

        String generateCaptionWithCTA = instaGramService.generateCaptionWithCTA(request.getContentIdea()
                ,request.getToneStyle(),request.getCallToAction());

        // Returning a JSON response  not using the instaService currently will integrate when we integrate Langchain AI
        Map<String, String> response = new HashMap<>();
        response.put("caption", generateCaptionWithCTA);
        return response;
    }

    @PostMapping("/insta_three")
    public Map<String, List<String>> generateHashtags(@RequestBody HashtagsDTO request) {
        List<String> generatedHashtags = new ArrayList<>();

        // Simulating hashtag generation (Later, we will integrate LangChain AI)
        String generatedHashtags1 =  instaGramService.suggestHashtags(request.getNiche(),request.getKeywords(),
                request.getTrendingOrEvergreen());
//        for (String keyword : request.getKeywords()) {
//            generatedHashtags.add("#" + keyword.replace(" ", "").toLowerCase());
//        }
//        generatedHashtags.add("#" + request.getNiche().replace(" ", "").toLowerCase());
//
//        if ("Trending".equalsIgnoreCase(request.getTrendingOrEvergreen())) {
//            generatedHashtags.add("#trending");
//        } else {
//            generatedHashtags.add("#evergreencontent");
//        }

        // Returning a JSON response / not using the instaService currently will integrate when we integrate Langchain AI
        Map<String, List<String>> response = new HashMap<>();
        response.put("hashtags", generatedHashtags);
        return response;
    }


    @PostMapping("/insta_four")
    public Map<String, String> generateDesignAndSuggestion(@RequestBody DesignDTO request) {

        // Simulated response (Later, we will integrate LangChain AI)

        String generateDesignAestheticSugestion =instaGramService.suggestDesignAndAesthetic(request.getNiche(),
                request.getToneStyle(),request.getContentType());

        // Returning a JSON response not using the instaService currently will integrate when we integrate Langchain AI
        Map<String, String> response = new HashMap<>();
        response.put("hook", generateDesignAestheticSugestion);
        return response;
    }


    @PostMapping("/insta_five")
    public Map<String, String> generateStrategyForEngagement(@RequestBody EngagementDTO request) {
        // Simulated response (Later, we will integrate LangChain AI)
        String generateEngagementTips = instaGramService.generateEngagementStrategies(request.getContentGoal(),request.getTargetAudience());

        // Returning a JSON response
        Map<String, String> response = new HashMap<>();
        response.put("strategy", generateEngagementTips);
        return response;
    }

    @PostMapping("/insta_six")
    public Map<String, String> generateContentPostingTime(@RequestBody PostingTimeDTO request) {
        // Simulated response (Later, we will integrate LangChain AI)
        String generateBestPostingTime = instaGramService.suggestBestPostTime(request.getNiche(),request.getTargetAudience());

        // Returning a JSON response
        Map<String, String> response = new HashMap<>();
        response.put("contentAdvice", generateBestPostingTime);
        return response;
    }


}
