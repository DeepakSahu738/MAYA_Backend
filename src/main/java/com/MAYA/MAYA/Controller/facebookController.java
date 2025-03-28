package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.facebook.FacebookPostDTO;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceFacebook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/facebook")
@CrossOrigin(origins = "http://localhost:9090")
public class facebookController {

    @Autowired
    private LangChainAiServiceFacebook langChainAiServiceFacebook ;
    @PostMapping("/FB_one")
    private ResponseEntity<Map<String, List<String>>> generateContentIdea(@RequestBody FacebookPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            List<String> generatePostIdeas = langChainAiServiceFacebook.generatePostIdeas(request.getPostGoal(),request.getNiche());
//            String contentIdea = generatePostIdeas;
//            storageService.storeContentIdea(sessionId,contentIdea);
            Map<String, List<String>> response = new HashMap<>();
              response.put("PostIdeas", generatePostIdeas);
//            response.put("sessionId", sessionId);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, List<String>> response = new HashMap<>();
            response.put("ERROR", Collections.singletonList("Error: Unable to generate Post ideas. Please try again later."));
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/FB_two")
    private ResponseEntity<Map<String, String>> generateHeadlinesAndDes(@RequestBody FacebookPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            String generateHeadlinesAndDes = langChainAiServiceFacebook.generateHeadlinesAndDes(request.getPostType(),request.getToneAndStyle(),request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("HeadlinesAndDes", generateHeadlinesAndDes);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate HeadlinesAndDes. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/FB_three")
    private ResponseEntity<Map<String, String>> suggestHashtags(@RequestBody FacebookPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            String suggestHashtags = langChainAiServiceFacebook.suggestHashtags(request.getTopicsAndKeywords());
            Map<String, String> response = new HashMap<>();
            response.put("Hashtags", suggestHashtags);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR","Error: Unable to generate Hashtags. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/FB_four")
    private ResponseEntity<Map<String, String>> suggestEngagementFeatures(@RequestBody FacebookPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            String suggestEngagementFeatures = langChainAiServiceFacebook.suggestEngagementFeatures(request.getPostType());
            Map<String, String> response = new HashMap<>();
            response.put("EngagementFeatures", suggestEngagementFeatures);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate EngagementFeatures. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/FB_five")
    private ResponseEntity<Map<String, String>> generateBoostingTips(@RequestBody FacebookPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            String generateBoostingTips = langChainAiServiceFacebook.generateBoostingTips();
            Map<String, String> response = new HashMap<>();
            response.put("BoostingTips", generateBoostingTips);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate BoostingTips. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/FB_six")
    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody FacebookPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            String suggestBestPostTime = langChainAiServiceFacebook.suggestBestPostTime(request.getTargetAudience());
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
