package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.twitterX.TwitterPostDTO;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceX;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/x")
@CrossOrigin(origins = "http://localhost:9090")
public class XController {

    @Autowired
    private LangChainAiServiceX langChainAiServiceX;

    @PostMapping("/x_one")
    private ResponseEntity<Map<String, List<String>>> generateTweetIdeas(@RequestBody TwitterPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            List<String> generateTweetIdeas = langChainAiServiceX.generateTweetIdeas(request.getTweetGoal(),request.getNiche(),request.getTargetAudience(),request.getTweetType(),request.getContentType());
            Map<String, List<String>> response = new HashMap<>();
            response.put("TweetIdeas", generateTweetIdeas);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, List<String>> response = new HashMap<>();
            response.put("ERROR", Collections.singletonList("Error: Unable to generate TweetIdeas. Please try again later."));
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/x_two")
    private ResponseEntity<Map<String, String>> generateOptimizedTweetCopy(@RequestBody TwitterPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateOptimizedTweetCopy = langChainAiServiceX.generateOptimizedTweetCopy(request.getToneAndStyle(),request.getHashtagsAndMentions(),request.getCallToAction(),request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("OptimizedTweetCopy", generateOptimizedTweetCopy);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate OptimizedTweetCopy. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/x_three")
    private ResponseEntity<Map<String, String>> suggestHashtag(@RequestBody TwitterPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestHashtag = langChainAiServiceX.suggestHashtag(request.getHashtagsAndMentions(),request.getNiche(),request.getContentType());
            Map<String, String> response = new HashMap<>();
            response.put("Hashtag", suggestHashtag);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR","Error: Unable to generate Hashtag. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/x_four")
    private ResponseEntity<Map<String, String>> suggestVisualAndGIFSuggestions(@RequestBody TwitterPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestVisualAndGIFSuggestions = langChainAiServiceX.suggestVisualAndGIFSuggestions(request.getTweetType(),request.getToneAndStyle(),request.getNiche(),request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("VisualAndGIFSuggestions", suggestVisualAndGIFSuggestions);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate VisualAndGIFSuggestions. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/x_five")
    private ResponseEntity<Map<String, String>> generateEngagement(@RequestBody TwitterPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateEngagement = langChainAiServiceX.generateEngagement(request.getTargetAudience(), request.getNiche());
            Map<String, String> response = new HashMap<>();
            response.put("Engagement", generateEngagement);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate Engagement. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/x_six")
    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody TwitterPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestBestPostTime = langChainAiServiceX.suggestBestPostTime(request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("BestPostTime", suggestBestPostTime);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate BestPostTime. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
