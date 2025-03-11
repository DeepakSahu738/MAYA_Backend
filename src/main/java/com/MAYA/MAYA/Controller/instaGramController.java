package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.instagram.*;
import com.MAYA.MAYA.Service.TemporaryStorageService;
import com.MAYA.MAYA.Service.instaGramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content")
@CrossOrigin(origins = "http://localhost:9090")
public class instaGramController {

    @Autowired
    private instaGramService instaGramService;
    @Autowired
    private TemporaryStorageService storageService;
    @PostMapping("/insta_one")
    private ResponseEntity<Map<String, String>> generateContentIdea(@RequestBody ContentIdeaDTO request) {
        //we are doing the LangChain stuff in the service section

        String sessionId = UUID.randomUUID().toString();

        try {
            String generateCoreIdea = instaGramService.generateContentIdeas(request.getContentGoal(),request.getNiche(),request.getContentType()
                    ,request.getTrendingOrEvergreen(),request.getTargetAudience());
            String contentIdea = generateCoreIdea;
            storageService.storeContentIdea(sessionId,contentIdea);
            Map<String, String> response = new HashMap<>();
            response.put("contentIdea", generateCoreIdea);
            response.put("sessionId", sessionId);
            return  new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {

            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/insta_two")
    private ResponseEntity<Map<String, String>> generateCaption(@RequestParam String sessionId,@RequestBody CaptionDTO request) {

        //we are doing the LangChain stuff in the service section
        String contentIdea = storageService.getContentIdea(sessionId);
        if (contentIdea == null) {
            Map<String, String> errorForSession = new HashMap<>();
            errorForSession.put("sessionError", "Session expired or invalid");
            return  new ResponseEntity<>(errorForSession, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            String generateCaptionWithCTA = instaGramService.generateCaptionWithCTA(contentIdea
                    , request.getToneStyle(), request.getCallToAction());
            Map<String, String> response = new HashMap<>();
            response.put("caption", generateCaptionWithCTA);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e){
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // Returning a JSON response  not using the instaService currently will integrate when we integrate Langchain AI
    }

    @PostMapping("/insta_three")
    private ResponseEntity<Map<String, String>> generateHashtags(@RequestBody HashtagsDTO request) {
        List<String> generatedHashtags = new ArrayList<>();

        //we are doing the LangChain stuff in the service section
        try {
            String generatedHashtags1 = instaGramService.suggestHashtags(request.getNiche(), request.getKeywords(),
                    request.getTrendingOrEvergreen());
            Map<String, String> response = new HashMap<>();
            response.put("hashtags", generatedHashtags1);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e){
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", " Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
    }


    @PostMapping("/insta_four")
    private ResponseEntity<Map<String, String>> generateDesignAndSuggestion(@RequestBody DesignDTO request) {

        //we are doing the LangChain stuff in the service section
        try {
            String generateDesignAestheticSugestion = instaGramService.suggestDesignAndAesthetic(request.getNiche(),
                    request.getToneStyle(), request.getContentType());
            Map<String, String> response = new HashMap<>();
            response.put("hook", generateDesignAestheticSugestion);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e) {
            // Returning a JSON response not using the instaService currently will integrate when we integrate Langchain AI
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/insta_five")
    private ResponseEntity<Map<String, String>> generateStrategyForEngagement(@RequestBody EngagementDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateEngagementTips = instaGramService.generateEngagementStrategies(request.getContentGoal(), request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("strategy", generateEngagementTips);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        // Returning a JSON response
        catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/insta_six")
    private ResponseEntity<Map<String, String>> generateContentPostingTime(@RequestBody PostingTimeDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateBestPostingTime = instaGramService.suggestBestPostTime(request.getNiche(), request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("contentAdvice", generateBestPostingTime);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        // Returning a JSON response
        catch (Exception e){
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
