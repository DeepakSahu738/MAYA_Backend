package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.tiktok.EngagementPojo.ResponsePOJOTTEngagementWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.ResponsePOJOTTCombinedPOJO;
import com.MAYA.MAYA.DTO.tiktok.TikTokPostDTO;
import com.MAYA.MAYA.DTO.tiktok.hashtagPojo.ResponsePOJOTTHashtagsWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.hooksAndCaptionsPojo.ResponsePOJOTTHooksAndCaptionsWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.musicAndEffectPojo.ResponsePOJOTTMusicAndEffetWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.postingTimePojo.ResponsePOJOTTPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.videoIdeaPojo.ResponsePOJOTTVideoIdea;
import com.MAYA.MAYA.DTO.tiktok.videoIdeaPojo.ResponsePOJOTTVideoIdeaWRAPPER;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceTikTok;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/tiktok")
@CrossOrigin(origins = {"http://localhost:5173","https://mayamanage.com","https://mayamanage-84da8.firebaseapp.com",
        "https://mayamanage-84da8.web.app"})
public class tiktokController {

    @Autowired
    private LangChainAiServiceTikTok langChainAiServiceTikTok;

    @PostMapping("/tt_prime")
    private ResponseEntity<Map<String, ResponsePOJOTTCombinedPOJO>> generateVideoIdeas(@RequestBody TikTokPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        ResponsePOJOTTCombinedPOJO combinedPOJO = new ResponsePOJOTTCombinedPOJO();
        try {
            ResponsePOJOTTVideoIdeaWRAPPER generateVideoIdeas = langChainAiServiceTikTok.generateVideoIdeas(request.getNiche(),request.getVideoFormat(),request.getVideoGoal(),request.getTargetAudience(),request.getContentType());
            ResponsePOJOTTHooksAndCaptionsWRAPPER generateHooksAndCaption = langChainAiServiceTikTok.generateHooksAndCaption(request.getToneAndStyle(),request.getCallToAction(),request.getNiche());
            ResponsePOJOTTHashtagsWRAPPER suggestHashtag = langChainAiServiceTikTok.suggestHashtag(request.getVideoFormat(),request.getContentType(),request.getNiche(),request.getTargetAudience());
            ResponsePOJOTTMusicAndEffetWRAPPER suggestMusicAndEffect = langChainAiServiceTikTok.suggestMusicAndEffect(request.getToneAndStyle(),request.getVideoFormat(),request.getNiche());
            ResponsePOJOTTEngagementWRAPPER generateEngagement = langChainAiServiceTikTok.generateEngagement(request.getVideoGoal(),request.getTargetAudience());
            ResponsePOJOTTPostingTimeWRAPPER suggestBestPostTime = langChainAiServiceTikTok.suggestBestPostTime(request.getTargetAudience());

            combinedPOJO.setVideoIdeaList(generateVideoIdeas);
            combinedPOJO.setHooksAndCaptionsList(generateHooksAndCaption);
            combinedPOJO.setHashtagsList(suggestHashtag);
            combinedPOJO.setMusicAndEffetList(suggestMusicAndEffect);
            combinedPOJO.setEngagementTacticList(generateEngagement);
            combinedPOJO.setPostingTimeList(suggestBestPostTime);

            Map<String, ResponsePOJOTTCombinedPOJO> response = new HashMap<>();
            response.put("VideoIdeasData", combinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ResponsePOJOTTVideoIdea erroridea = new ResponsePOJOTTVideoIdea();
            ResponsePOJOTTVideoIdeaWRAPPER errorideaWRAPPER = new ResponsePOJOTTVideoIdeaWRAPPER();

            erroridea.setVideoIdea("Error: Unable to generate VideoIdeas. Please try again later."+e.getMessage());
            List<ResponsePOJOTTVideoIdea> videoIdeas = new ArrayList<>();
            videoIdeas.add(erroridea);
            errorideaWRAPPER.setVideoIdeas(videoIdeas);
            combinedPOJO.setVideoIdeaList(errorideaWRAPPER);
            Map<String, ResponsePOJOTTCombinedPOJO> response = new HashMap<>();
            response.put("ERROR", combinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
//    @PostMapping("/tt_two")
//    private ResponseEntity<Map<String, String>> generateHooksAndCaption(@RequestBody TikTokPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateHooksAndCaption = langChainAiServiceTikTok.generateHooksAndCaption(request.getToneAndStyle(),request.getCallToAction(),request.getNiche());
//            Map<String, String> response = new HashMap<>();
//            response.put("HooksAndCaption", generateHooksAndCaption);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate HooksAndCaption. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/tt_three")
//    private ResponseEntity<Map<String, String>> suggestHashtag(@RequestBody TikTokPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestHashtag = langChainAiServiceTikTok.suggestHashtag(request.getVideoFormat(),request.getContentType(),request.getNiche(),request.getTargetAudience());
//            Map<String, String> response = new HashMap<>();
//            response.put("Hashtag", suggestHashtag);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR","Error: Unable to generate Hashtag. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/tt_four")
//    private ResponseEntity<Map<String, String>> suggestMusicAndEffect(@RequestBody TikTokPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestMusicAndEffect = langChainAiServiceTikTok.suggestMusicAndEffect(request.getToneAndStyle(),request.getVideoFormat(),request.getNiche());
//            Map<String, String> response = new HashMap<>();
//            response.put("MusicAndEffect", suggestMusicAndEffect);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate MusicAndEffect. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/tt_five")
//    private ResponseEntity<Map<String, String>> generateEngagement(@RequestBody TikTokPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateEngagement = langChainAiServiceTikTok.generateEngagement(request.getVideoGoal(),request.getTargetAudience());
//            Map<String, String> response = new HashMap<>();
//            response.put("Engagement", generateEngagement);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate Engagement. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/tt_six")
//    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody TikTokPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestBestPostTime = langChainAiServiceTikTok.suggestBestPostTime(request.getTargetAudience());
//            Map<String, String> response = new HashMap<>();
//            response.put("BestPostTime", suggestBestPostTime);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate BestPostTime. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
}
