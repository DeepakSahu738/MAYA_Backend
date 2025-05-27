package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.youtube.ResponsePOJOYTCombinedPOJO;
import com.MAYA.MAYA.DTO.youtube.YouTubePostDTO;
import com.MAYA.MAYA.DTO.youtube.engagementPojo.ResponsePOJOYTEngagementWRAPPER;
import com.MAYA.MAYA.DTO.youtube.hashtagPojo.ResponsePOJOYTHashtagWRAPPER;
import com.MAYA.MAYA.DTO.youtube.postingTime.ResponsePOJOYTPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.youtube.seoPojo.ResponsePOJOYTSEOWRAPPER;
import com.MAYA.MAYA.DTO.youtube.thumbnailAndBranding.ResponsePOJOYTThumbnailAndBrandingWRAPPER;
import com.MAYA.MAYA.DTO.youtube.videoIdeaPojo.ResponsePOJOYTVideoIdea;
import com.MAYA.MAYA.DTO.youtube.videoIdeaPojo.ResponsePOJOYTVideoIdeaWRAPPER;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceYoutube;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/youtube")
@CrossOrigin(origins = "http://localhost:5173")
public class youtubeController {

    @Autowired
    private LangChainAiServiceYoutube langChainAiServiceYoutube;

    @PostMapping("yt_prime")
    private ResponseEntity<Map<String, ResponsePOJOYTCombinedPOJO>> generateVideoIdeas(@RequestBody YouTubePostDTO request) {
        //we are doing the LangChain stuff in the service section
        String sessionId = UUID.randomUUID().toString();
        ResponsePOJOYTCombinedPOJO combinedPOJO = new ResponsePOJOYTCombinedPOJO();
        try {
            ResponsePOJOYTVideoIdeaWRAPPER generateVideoIdeas = langChainAiServiceYoutube.generateVideoIdeas(request.getVideoGoal(),request.getNiche(),request.getVideoType(),request.getTargetAudience(),request.getContentType());
            ResponsePOJOYTSEOWRAPPER generateSEO = langChainAiServiceYoutube.generateSEO(request.getKeywordsAndSeoTags(),request.getCallToAction());
            ResponsePOJOYTHashtagWRAPPER suggestHashtag = langChainAiServiceYoutube.suggestHashtag(request.getKeywordsAndSeoTags(),request.getNiche(),request.getContentType());
            ResponsePOJOYTThumbnailAndBrandingWRAPPER suggestThumbnailAndBranding = langChainAiServiceYoutube.suggestThumbnailAndBranding(request.getToneAndStyle(),request.getNiche(),request.getVideoGoal());
            ResponsePOJOYTEngagementWRAPPER generateEngagement = langChainAiServiceYoutube.generateEngagement(request.getTargetAudience(), request.getNiche());
            ResponsePOJOYTPostingTimeWRAPPER suggestBestPostTime = langChainAiServiceYoutube.suggestBestPostTime(request.getTargetAudience());

            combinedPOJO.setVideoIdeaList(generateVideoIdeas);
            combinedPOJO.setSeoList(generateSEO);
            combinedPOJO.setHashtagList(suggestHashtag);
            combinedPOJO.setThumbnailAndBrandingList(suggestThumbnailAndBranding);
            combinedPOJO.setEngagementList(generateEngagement);
            combinedPOJO.setPostingTimeList(suggestBestPostTime);
            Map<String, ResponsePOJOYTCombinedPOJO> response = new HashMap<>();
            response.put("VideoIdeasData", combinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ResponsePOJOYTVideoIdea erroridea = new ResponsePOJOYTVideoIdea();
            ResponsePOJOYTVideoIdeaWRAPPER errorideaList = new ResponsePOJOYTVideoIdeaWRAPPER();
            erroridea.setVideoIdea("Error: Unable to generate VideoIdeas. Please try again later."+e.getMessage());
            List<ResponsePOJOYTVideoIdea> videoIdeas = new ArrayList<>();
            videoIdeas.add(erroridea);
            errorideaList.setVideoIdeas(videoIdeas);
            combinedPOJO.setVideoIdeaList(errorideaList);
            Map<String, ResponsePOJOYTCombinedPOJO> response = new HashMap<>();
            response.put("ERROR", combinedPOJO);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @PostMapping("/yt_two")
//    private ResponseEntity<Map<String, String>> generateSEO(@RequestBody YouTubePostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateSEO = langChainAiServiceYoutube.generateSEO(request.getKeywordsAndSeoTags(),request.getCallToAction());
//            Map<String, String> response = new HashMap<>();
//            response.put("SEO", generateSEO);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate SEO. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/yt_three")
//    private ResponseEntity<Map<String, String>> suggestHashtag(@RequestBody YouTubePostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestHashtag = langChainAiServiceYoutube.suggestHashtag(request.getKeywordsAndSeoTags(),request.getNiche(),request.getContentType());
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
//    @PostMapping("/yt_four")
//    private ResponseEntity<Map<String, String>> suggestThumbnailAndBranding(@RequestBody YouTubePostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestThumbnailAndBranding = langChainAiServiceYoutube.suggestThumbnailAndBranding(request.getToneAndStyle(),request.getNiche(),request.getVideoGoal());
//            Map<String, String> response = new HashMap<>();
//            response.put("ThumbnailAndBranding", suggestThumbnailAndBranding);
//            return  new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            Map<String, String> response = new HashMap<>();
//            response.put("ERROR", "Error: Unable to generate ThumbnailAndBranding. Please try again later.");
//            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PostMapping("/yt_five")
//    private ResponseEntity<Map<String, String>> generateEngagement(@RequestBody YouTubePostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String generateEngagement = langChainAiServiceYoutube.generateEngagement(request.getTargetAudience(), request.getNiche());
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
//    @PostMapping("/x_six")
//    private ResponseEntity<Map<String, String>> suggestBestPostTime(@RequestBody YouTubePostDTO request) {
//        //we are doing the LangChain stuff in the service section
//        try {
//            String suggestBestPostTime = langChainAiServiceYoutube.suggestBestPostTime(request.getTargetAudience());
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
