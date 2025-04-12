package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.facebook.FacebookPostDTO;
import com.MAYA.MAYA.DTO.facebook.ResponsePOJOFBConbinedPOJO;
import com.MAYA.MAYA.DTO.facebook.adAndBoostingPojo.ResponsePOJOFBadAndBoostingWRAPPER;
import com.MAYA.MAYA.DTO.facebook.engagementFeaturePojo.ResponsePOJOFBengagementFeatureWRAPPER;
import com.MAYA.MAYA.DTO.facebook.hashtagsPojo.ResponsePOJOFBhashtagsWRAPPER;
import com.MAYA.MAYA.DTO.facebook.headlinesAndDesPojo.ResponsePOJOFBheadlinesAndDesWRAPPER;
import com.MAYA.MAYA.DTO.facebook.postIdeaPojo.ResponsePOJOFBPostIdea;
import com.MAYA.MAYA.DTO.facebook.postIdeaPojo.ResponsePOJOFBPostIdeaWRAPPER;
import com.MAYA.MAYA.DTO.facebook.postingTimePojo.ResponsePOJOFBPostingTimeWRAPPER;
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
    @PostMapping("/fb_prime")
    private ResponseEntity<Map<String, ResponsePOJOFBConbinedPOJO>> generateContentIdea(@RequestBody FacebookPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        ResponsePOJOFBConbinedPOJO combinedPOJO = new ResponsePOJOFBConbinedPOJO();
        try {
            ResponsePOJOFBPostIdeaWRAPPER generatePostIdeas = langChainAiServiceFacebook.generatePostIdeas(request.getPostGoal(),request.getNiche());
            ResponsePOJOFBheadlinesAndDesWRAPPER generateHeadlinesAndDes = langChainAiServiceFacebook.generateHeadlinesAndDes(request.getPostType(),request.getToneAndStyle(),request.getTargetAudience());
            ResponsePOJOFBhashtagsWRAPPER suggestHashtags = langChainAiServiceFacebook.suggestHashtags(request.getTopicsAndKeywords());
            ResponsePOJOFBengagementFeatureWRAPPER suggestEngagementFeatures = langChainAiServiceFacebook.suggestEngagementFeatures(request.getPostType());
            ResponsePOJOFBadAndBoostingWRAPPER generateBoostingTips = langChainAiServiceFacebook.generateBoostingTips();
            ResponsePOJOFBPostingTimeWRAPPER suggestBestPostTime = langChainAiServiceFacebook.suggestBestPostTime(request.getTargetAudience());

            combinedPOJO.setpostIdeasList(generatePostIdeas);
            combinedPOJO.setheadlinesAndDesList(generateHeadlinesAndDes);
            combinedPOJO.sethashtagsList(suggestHashtags);
            combinedPOJO.setengagementFeatureList(suggestEngagementFeatures);
            combinedPOJO.setadAndBoostingList(generateBoostingTips);
            combinedPOJO.setPostingTimeList(suggestBestPostTime);

            Map<String, ResponsePOJOFBConbinedPOJO> response = new HashMap<>();
              response.put("facebookData", combinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ResponsePOJOFBPostIdea idea = new ResponsePOJOFBPostIdea();
            ResponsePOJOFBPostIdeaWRAPPER ideaWRAPPER = new ResponsePOJOFBPostIdeaWRAPPER();
            idea.setPostIdea("Error: Unable to generate Post ideas. Please try again later.");
            ideaWRAPPER.setPostIdeas((List<ResponsePOJOFBPostIdea>) idea);
            combinedPOJO.setpostIdeasList(ideaWRAPPER);
            Map<String, ResponsePOJOFBConbinedPOJO> response = new HashMap<>();
            response.put("ERROR", combinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @PostMapping("/fb_two")
//    private ResponseEntity<Map<String, String>> generateHeadlinesAndDes(@RequestBody FacebookPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateHeadlinesAndDes = langChainAiServiceFacebook.generateHeadlinesAndDes(request.getPostType(),request.getToneAndStyle(),request.getTargetAudience());
//            Map<String, String> response = new HashMap<>();
//            response.put("HeadlinesAndDes", generateHeadlinesAndDes);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate HeadlinesAndDes. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/fb_three")
//    private ResponseEntity<Map<String, String>> suggestHashtags(@RequestBody FacebookPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestHashtags = langChainAiServiceFacebook.suggestHashtags(request.getTopicsAndKeywords());
//            Map<String, String> response = new HashMap<>();
//            response.put("Hashtags", suggestHashtags);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR","Error: Unable to generate Hashtags. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/fb_four")
//    private ResponseEntity<Map<String, String>> suggestEngagementFeatures(@RequestBody FacebookPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestEngagementFeatures = langChainAiServiceFacebook.suggestEngagementFeatures(request.getPostType());
//            Map<String, String> response = new HashMap<>();
//            response.put("EngagementFeatures", suggestEngagementFeatures);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate EngagementFeatures. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/fb_five")
//    private ResponseEntity<Map<String, String>> generateBoostingTips(@RequestBody FacebookPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateBoostingTips = langChainAiServiceFacebook.generateBoostingTips();
//            Map<String, String> response = new HashMap<>();
//            response.put("BoostingTips", generateBoostingTips);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate BoostingTips. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/fb_six")
//    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody FacebookPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestBestPostTime = langChainAiServiceFacebook.suggestBestPostTime(request.getTargetAudience());
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
