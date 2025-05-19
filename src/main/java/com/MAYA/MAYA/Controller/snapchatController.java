package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.snapchat.EngagementFeaturesPojo.ResponsePOJOSCEngagementFeaturesWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.ResponsePOJOSCConbinedPOJO;
import com.MAYA.MAYA.DTO.snapchat.SnapchatPostDTO;
import com.MAYA.MAYA.DTO.snapchat.boostingTipsPojo.ResponsePOJOSCBoostingTipsWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.filtersPojo.ResponsePOJOSClensesAndFiltersWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.postingTimePojo.ResponsePOJOSCPOstingTimeWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.storyIdeaPojo.ResponsePOJOSCStoryIdea;
import com.MAYA.MAYA.DTO.snapchat.storyIdeaPojo.ResponsePOJOSCStoryIdeaWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.textOverlaysPojo.ResponsePOJOSCTextOverlaysWRAPPER;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceSnapchat;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/snapchat")
@CrossOrigin(origins = "http://localhost:5173")
public class snapchatController {

    @Autowired
    private LangChainAiServiceSnapchat langChainAiServiceSnapchat;

    @PostMapping("/sc_prime")
    private ResponseEntity<Map<String, ResponsePOJOSCConbinedPOJO>> generateStoryIdeas(@RequestBody SnapchatPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        ResponsePOJOSCConbinedPOJO conbinedPOJO =new ResponsePOJOSCConbinedPOJO();
        try {
            ResponsePOJOSCStoryIdeaWRAPPER generateStoryIdeas = langChainAiServiceSnapchat.generateStoryIdeas(request.getSnapGoal(),request.getNiche(),request.getStoryType(),request.getToneAndStyle(),request.getCallToAction(),request.getContentType());
            ResponsePOJOSCTextOverlaysWRAPPER generateTextOverlays = langChainAiServiceSnapchat.generateTextOverlays(request.getStoryType(),request.getToneAndStyle(),request.getNiche(),request.getCallToAction(),request.getStickersAndFilters(),request.getTargetAudience());
            ResponsePOJOSClensesAndFiltersWRAPPER suggestTrendingLensesAndFilters = langChainAiServiceSnapchat.suggestTrendingLensesAndFilters(request.getNiche(),request.getContentType(),request.getStoryType(),request.getTargetAudience(),request.getStickersAndFilters());
            ResponsePOJOSCEngagementFeaturesWRAPPER suggestEngagementFeatures = langChainAiServiceSnapchat.suggestEngagementFeatures(request.getToneAndStyle(),request.getStoryType(),request.getNiche());
            ResponsePOJOSCBoostingTipsWRAPPER generateBoostingTips = langChainAiServiceSnapchat.generateBoostingTips();
            ResponsePOJOSCPOstingTimeWRAPPER suggestBestPostTime = langChainAiServiceSnapchat.suggestBestPostTime(request.getTargetAudience(),request.getStoryType());


            conbinedPOJO.setStoryIdeaList(generateStoryIdeas);
            conbinedPOJO.setTextOverlaysList(generateTextOverlays);
            conbinedPOJO.setLensesAndFiltersList(suggestTrendingLensesAndFilters);
            conbinedPOJO.setEngagementFeaturesList(suggestEngagementFeatures);
            conbinedPOJO.setBoostingTipsList(generateBoostingTips);
            conbinedPOJO.setPostingTimeList(suggestBestPostTime);

            Map<String, ResponsePOJOSCConbinedPOJO> response = new HashMap<>();
            response.put("StoryIdeasData", conbinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {


            ResponsePOJOSCStoryIdea story = new ResponsePOJOSCStoryIdea();
            ResponsePOJOSCStoryIdeaWRAPPER storyWRAPPER = new ResponsePOJOSCStoryIdeaWRAPPER();
            story.setStoryIdea("Error: Unable to generate StoryIdeas. Please try again later.");
            storyWRAPPER.setStoryIdeas((List<ResponsePOJOSCStoryIdea>) story);
            conbinedPOJO.setStoryIdeaList(storyWRAPPER);
            Map<String, ResponsePOJOSCConbinedPOJO> response = new HashMap<>();
            response.put("ERROR", conbinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @PostMapping("/sc_two")
//    private ResponseEntity<Map<String, String>> generateTextOverlays(@RequestBody SnapchatPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateTextOverlays = langChainAiServiceSnapchat.generateTextOverlays(request.getStoryType(),request.getToneAndStyle(),request.getNiche(),request.getCallToAction(),request.getStickersAndFilters(),request.getTargetAudience());
//            Map<String, String> response = new HashMap<>();
//            response.put("TextOverlays", generateTextOverlays);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate TextOverlays. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/sc_three")
//    private ResponseEntity<Map<String, String>> suggestTrendingLensesAndFilters(@RequestBody SnapchatPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestTrendingLensesAndFilters = langChainAiServiceSnapchat.suggestTrendingLensesAndFilters(request.getNiche(),request.getContentType(),request.getStoryType(),request.getTargetAudience(),request.getStickersAndFilters());
//            Map<String, String> response = new HashMap<>();
//            response.put("TrendingLensesAndFilters", suggestTrendingLensesAndFilters);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR","Error: Unable to generate TrendingLensesAndFilters. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/sc_four")
//    private ResponseEntity<Map<String, String>> suggestEngagementFeatures(@RequestBody SnapchatPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestEngagementFeatures = langChainAiServiceSnapchat.suggestEngagementFeatures(request.getToneAndStyle(),request.getStoryType(),request.getNiche());
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
//    @PostMapping("/sc_five")
//    private ResponseEntity<Map<String, String>> generateBoostingTips(@RequestBody SnapchatPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateBoostingTips = langChainAiServiceSnapchat.generateBoostingTips();
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
//    @PostMapping("/sc_six")
//    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody SnapchatPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestBestPostTime = langChainAiServiceSnapchat.suggestBestPostTime(request.getTargetAudience(),request.getStoryType());
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
