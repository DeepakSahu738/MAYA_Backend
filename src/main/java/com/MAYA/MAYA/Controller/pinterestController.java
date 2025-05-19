package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.pinterest.PinterestPostDTO;
import com.MAYA.MAYA.DTO.pinterest.ResponsePOJOPCombinedPOJO;
import com.MAYA.MAYA.DTO.pinterest.aestheticAndDesign.ResponsePOJOPAestheticAndDesignWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.engagement.ResponsePOJOPEngagementWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.hashtag.ResponsePOJOPHashtagWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.pinIdea.ResponsePOJOPPinIdea;
import com.MAYA.MAYA.DTO.pinterest.pinIdea.ResponsePOJOPPinIdeaWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.postingTime.ResponsePOJOPPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.titleAndDescription.ResponsePOJOPTitleAndDescriptionWRAPPER;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServicePinterest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/pinterest")
@CrossOrigin(origins = "http://localhost:5173")
public class pinterestController {
    @Autowired
    private LangChainAiServicePinterest langChainAiServicePinterest ;
    @PostMapping("/pin_prime")
    private ResponseEntity<Map<String, ResponsePOJOPCombinedPOJO>> generatePinIdeas(@RequestBody PinterestPostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        ResponsePOJOPCombinedPOJO combinedPOJO = new ResponsePOJOPCombinedPOJO();
        try {
            ResponsePOJOPPinIdeaWRAPPER generatePinIdeas = langChainAiServicePinterest.generatePinIdeas(request.getPinGoal(),request.getContentType(),request.getPinType(),request.getTargetAudience());
            ResponsePOJOPTitleAndDescriptionWRAPPER generateOptimizedTitleAndDescription = langChainAiServicePinterest.generateOptimizedTitleAndDescription(request.getSearchKeywords(),request.getCallToAction());
            ResponsePOJOPHashtagWRAPPER suggestHashtag = langChainAiServicePinterest.suggestHashtag(request.getNiche(),request.getPinGoal());
            ResponsePOJOPAestheticAndDesignWRAPPER suggestAestheticAndDesignTips = langChainAiServicePinterest.suggestAestheticAndDesignTips(request.getToneAndStyle(),request.getPinType());
            ResponsePOJOPEngagementWRAPPER generateEngagement = langChainAiServicePinterest.generateEngagement(request.getNiche(),request.getCallToAction());
            ResponsePOJOPPostingTimeWRAPPER suggestBestPostTime = langChainAiServicePinterest.suggestBestPostTime(request.getTargetAudience());
            Map<String, ResponsePOJOPCombinedPOJO> response = new HashMap<>();

            combinedPOJO.setPinIdeaList(generatePinIdeas);
            combinedPOJO.setTitleAndDescriptionList(generateOptimizedTitleAndDescription);
            combinedPOJO.setHashtagList(suggestHashtag);
            combinedPOJO.setAestheticAndDesignList(suggestAestheticAndDesignTips);
            combinedPOJO.setEngagementList(generateEngagement);
            combinedPOJO.setPostingTimeList(suggestBestPostTime);

            response.put("PinIdeasData", combinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ResponsePOJOPPinIdeaWRAPPER errorList = new ResponsePOJOPPinIdeaWRAPPER();
            ResponsePOJOPPinIdea error = new ResponsePOJOPPinIdea();
            error.setPinIdea("Error: Unable to generate Pin Ideas. Please try again later.");
            errorList.setPinIdeas((List<ResponsePOJOPPinIdea>) error);
            combinedPOJO.setPinIdeaList(errorList);
            Map<String, ResponsePOJOPCombinedPOJO> response = new HashMap<>();
            response.put("ERROR",combinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @PostMapping("/pin_two")
//    private ResponseEntity<Map<String, String>> generateOptimizedTitleAndDescription(@RequestBody PinterestPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateOptimizedTitleAndDescription = langChainAiServicePinterest.generateOptimizedTitleAndDescription(request.getSearchKeywords(),request.getCallToAction());
//            Map<String, String> response = new HashMap<>();
//            response.put("OptimizedTitleAndDescription", generateOptimizedTitleAndDescription);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate Optimized Title And Description. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/pin_three")
//    private ResponseEntity<Map<String, String>> suggestHashtag(@RequestBody PinterestPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestHashtag = langChainAiServicePinterest.suggestHashtag(request.getNiche(),request.getPinGoal());
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
//    @PostMapping("/pin_four")
//    private ResponseEntity<Map<String, String>> suggestAestheticAndDesignTips(@RequestBody PinterestPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestAestheticAndDesignTips = langChainAiServicePinterest.suggestAestheticAndDesignTips(request.getToneAndStyle(),request.getPinType());
//            Map<String, String> response = new HashMap<>();
//            response.put("AestheticAndDesignTips", suggestAestheticAndDesignTips);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate AestheticAndDesignTips. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/pin_five")
//    private ResponseEntity<Map<String, String>> generateEngagement(@RequestBody PinterestPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateEngagement = langChainAiServicePinterest.generateEngagement(request.getNiche(),request.getCallToAction());
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
//    @PostMapping("/pin_six")
//    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody PinterestPostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestBestPostTime = langChainAiServicePinterest.suggestBestPostTime(request.getTargetAudience());
//            Map<String, String> response = new HashMap<>();
//            response.put("BestPostTime", suggestBestPostTime);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate BestPostTime. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//

}
