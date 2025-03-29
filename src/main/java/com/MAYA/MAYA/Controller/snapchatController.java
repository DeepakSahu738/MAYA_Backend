package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.snapchat.SnapchatPostDTO;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceSnapchat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/snapchat")
@CrossOrigin(origins = "http://localhost:9090")
public class snapchatController {

    @Autowired
    private LangChainAiServiceSnapchat langChainAiServiceSnapchat;

    @PostMapping("/sc_one")
    private ResponseEntity<Map<String, List<String>>> generateStoryIdeas(@RequestBody SnapchatPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            List<String> generateStoryIdeas = langChainAiServiceSnapchat.generateStoryIdeas(request.getSnapGoal(),request.getNiche(),request.getStoryType(),request.getToneAndStyle(),request.getCallToAction(),request.getContentType());
            Map<String, List<String>> response = new HashMap<>();
            response.put("StoryIdeas", generateStoryIdeas);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, List<String>> response = new HashMap<>();
            response.put("ERROR", Collections.singletonList("Error: Unable to generate StoryIdeas. Please try again later."));
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/sc_two")
    private ResponseEntity<Map<String, String>> generateTextOverlays(@RequestBody SnapchatPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateTextOverlays = langChainAiServiceSnapchat.generateTextOverlays(request.getStoryType(),request.getToneAndStyle(),request.getNiche(),request.getCallToAction(),request.getStickersAndFilters(),request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("TextOverlays", generateTextOverlays);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate TextOverlays. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/sc_three")
    private ResponseEntity<Map<String, String>> suggestTrendingLensesAndFilters(@RequestBody SnapchatPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestTrendingLensesAndFilters = langChainAiServiceSnapchat.suggestTrendingLensesAndFilters(request.getNiche(),request.getContentType(),request.getStoryType(),request.getTargetAudience(),request.getStickersAndFilters());
            Map<String, String> response = new HashMap<>();
            response.put("TrendingLensesAndFilters", suggestTrendingLensesAndFilters);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR","Error: Unable to generate TrendingLensesAndFilters. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/sc_four")
    private ResponseEntity<Map<String, String>> suggestEngagementFeatures(@RequestBody SnapchatPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestEngagementFeatures = langChainAiServiceSnapchat.suggestEngagementFeatures(request.getToneAndStyle(),request.getStoryType(),request.getNiche());
            Map<String, String> response = new HashMap<>();
            response.put("EngagementFeatures", suggestEngagementFeatures);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate EngagementFeatures. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/sc_five")
    private ResponseEntity<Map<String, String>> generateBoostingTips(@RequestBody SnapchatPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateBoostingTips = langChainAiServiceSnapchat.generateBoostingTips();
            Map<String, String> response = new HashMap<>();
            response.put("BoostingTips", generateBoostingTips);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate BoostingTips. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/sc_six")
    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody SnapchatPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestBestPostTime = langChainAiServiceSnapchat.suggestBestPostTime(request.getTargetAudience(),request.getStoryType());
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
