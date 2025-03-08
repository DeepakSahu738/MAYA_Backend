package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.*;
import com.MAYA.MAYA.Service.instaGramService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
@CrossOrigin(origins = "http://localhost:9090")
public class instaGramController {

    private instaGramService instaGramService;
    @PostMapping("/insta_one")
    public Map<String, String> generateContentIdea(@RequestBody  instaPrompt_ONE request) {

        // Simulated response (Later, we will integrate LangChain AI here)
        String generateCoreIdea = instaGramService.createPrompt_ONE(request.getContentGoal(),request.getNiche(),request.getContentType()
        ,request.getTrendingOrEvergreen(),request.getTargetAudience());

        // Returning a JSON response  not using the instaService currently will integrate when we integrate Langchain AI
        Map<String, String> response = new HashMap<>();
        response.put("contentIdea", generateCoreIdea);
        return response;
    }

    @PostMapping("/insta_two")
    public Map<String, String> generateCaption(@RequestBody instaPrompt_TWO request) {

        // Simulated response (Later, we will integrate LangChain AI here)

        String generateCaptionWithCTA = instaGramService.createPrompt_TWO(request.getContentIdea()
                ,request.getToneStyle(),request.getCallToAction());

        // Returning a JSON response  not using the instaService currently will integrate when we integrate Langchain AI
        Map<String, String> response = new HashMap<>();
        response.put("caption", generateCaptionWithCTA);
        return response;
    }

    @PostMapping("/insta_three")
    public Map<String, List<String>> generateHashtags(@RequestBody instaPrompt_THREE request) {
        List<String> generatedHashtags = new ArrayList<>();

        // Simulating hashtag generation (Later, we will integrate LangChain AI)
        String generatedHashtags1 =  instaGramService.createPrompt_THREE(request.getNiche(),request.getKeywords(),
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
    public Map<String, String> generateHook(@RequestBody instaPrompt_FOUR request) {

        // Simulated response (Later, we will integrate LangChain AI)

        String generateDesignAestheticSugestion =instaGramService.createPrompt_FOUR(request.getNiche(),
                request.getToneStyle(),request.getContentType());

        // Returning a JSON response not using the instaService currently will integrate when we integrate Langchain AI
        Map<String, String> response = new HashMap<>();
        response.put("hook", generateDesignAestheticSugestion);
        return response;
    }


    @PostMapping("/insta_five")
    public Map<String, String> generateStrategy(@RequestBody instaPrompt_FIVE request) {
        // Simulated response (Later, we will integrate LangChain AI)
        String generateEngagementTips = instaGramService.createPrompt_FIVE(request.getContentGoal(),request.getTargetAudience());

        // Returning a JSON response
        Map<String, String> response = new HashMap<>();
        response.put("strategy", generateEngagementTips);
        return response;
    }

    @PostMapping("/insta_six")
    public Map<String, String> generateContentAdvice(@RequestBody instaPrompt_SIX request) {
        // Simulated response (Later, we will integrate LangChain AI)
        String generateBestPostingTime = instaGramService.createPrompt_SIX(request.getNiche(),request.getTargetAudience());

        // Returning a JSON response
        Map<String, String> response = new HashMap<>();
        response.put("contentAdvice", generateBestPostingTime);
        return response;
    }


}
