package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.tiktok.TikTokPostDTO;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceTikTok;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/tiktok")
@CrossOrigin(origins = "http://localhost:9090")
public class tiktokController {

    @Autowired
    private LangChainAiServiceTikTok langChainAiServiceTikTok;

    @PostMapping("/tt_one")
    private ResponseEntity<Map<String, List<String>>> generateVideoIdeas(@RequestBody TikTokPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        try {
            List<String> generateVideoIdeas = langChainAiServiceTikTok.generateVideoIdeas(request.getNiche(),request.getVideoFormat(),request.getVideoGoal(),request.getTargetAudience(),request.getContentType());
            Map<String, List<String>> response = new HashMap<>();
            response.put("VideoIdeas", generateVideoIdeas);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, List<String>> response = new HashMap<>();
            response.put("ERROR", Collections.singletonList("Error: Unable to generate VideoIdeas. Please try again later."));
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/tt_two")
    private ResponseEntity<Map<String, String>> generateHooksAndCaption(@RequestBody TikTokPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateHooksAndCaption = langChainAiServiceTikTok.generateHooksAndCaption(request.getToneAndStyle(),request.getCallToAction(),request.getNiche());
            Map<String, String> response = new HashMap<>();
            response.put("HooksAndCaption", generateHooksAndCaption);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate HooksAndCaption. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/tt_three")
    private ResponseEntity<Map<String, String>> suggestHashtag(@RequestBody TikTokPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestHashtag = langChainAiServiceTikTok.suggestHashtag(request.getVideoFormat(),request.getContentType(),request.getNiche(),request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("Hashtag", suggestHashtag);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR","Error: Unable to generate Hashtag. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/tt_four")
    private ResponseEntity<Map<String, String>> suggestMusicAndEffect(@RequestBody TikTokPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestMusicAndEffect = langChainAiServiceTikTok.suggestMusicAndEffect(request.getToneAndStyle(),request.getVideoFormat(),request.getNiche());
            Map<String, String> response = new HashMap<>();
            response.put("MusicAndEffect", suggestMusicAndEffect);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate MusicAndEffect. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/tt_five")
    private ResponseEntity<Map<String, String>> generateEngagement(@RequestBody TikTokPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateEngagement = langChainAiServiceTikTok.generateEngagement(request.getVideoGoal(),request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("Engagement", generateEngagement);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate Engagement. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/tt_six")
    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody TikTokPostDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String suggestBestPostTime = langChainAiServiceTikTok.suggestBestPostTime(request.getTargetAudience());
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
