package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.instagram.*;
import com.MAYA.MAYA.DTO.instagram.captionPojo.ResponsePOJOInstaCaption;
import com.MAYA.MAYA.DTO.instagram.captionPojo.ResponsePOJOInstaCaptionWRAPPER;
import com.MAYA.MAYA.DTO.instagram.contentIdeaPojo.ResponsePOJOChainIdealSuggestion;
import com.MAYA.MAYA.DTO.instagram.contentIdeaPojo.ResponsePOJOInstaContentIdea;
import com.MAYA.MAYA.DTO.instagram.contentIdeaPojo.ResponsePOJOInstaContentIdeaWRAPPER;
import com.MAYA.MAYA.DTO.instagram.designAndAestheticPojo.ResponsePOJOInstaDesignAndAesthetic;
import com.MAYA.MAYA.DTO.instagram.designAndAestheticPojo.ResponsePOJOInstaDesignAndAestheticWRAPPER;
import com.MAYA.MAYA.DTO.instagram.engagementPojo.ResponsePOJOInstaEngagementStrategies;
import com.MAYA.MAYA.DTO.instagram.engagementPojo.ResponsePOJOInstaEngagementStrategiesWRAPPER;
import com.MAYA.MAYA.DTO.instagram.hashtagsPojo.ResponsePOJOInstaHashtags;
import com.MAYA.MAYA.DTO.instagram.hashtagsPojo.ResponsePOJOInstaHashtagsWRAPPER;
import com.MAYA.MAYA.DTO.instagram.postingTimePojo.ResponsePOJOInstaBestPostingTime;
import com.MAYA.MAYA.DTO.instagram.postingTimePojo.ResponsePOJOInstaBestPostingTimeWRAPPER;
import com.MAYA.MAYA.Service.TemporaryStorageService;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceInstagram;
import com.MAYA.MAYA.Service.contentServices.instaGramService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/content/instagram")
@CrossOrigin(origins = "http://localhost:9090")
public class instaGramController {

    @Autowired
    private instaGramService instaGramService;
    @Autowired
    private OpenAiChatModel OpenAiChatModel;
    @Autowired
    private TemporaryStorageService storageService;
    @Autowired
    private LangChainAiServiceInstagram langChainService;
    @PostMapping("/insta_one")
    private ResponseEntity<Map<String, ResponsePOJOInstaContentIdeaWRAPPER>> generateContentIdea(@RequestBody CombinedInstaDTO request) {
        //we are doing the LangChain stuff in the service section

        String sessionId = UUID.randomUUID().toString();
        //System.out.println("entered in the call");

        try {
            //System.out.println("entered in the try inside the call");
            ResponsePOJOInstaContentIdeaWRAPPER generateCoreIdea = langChainService.generateContentIdeas(request.getContentGoal(),request.getNiche(),request.getContentType()
                      ,request.getTrendingOrEvergreen(),request.getTargetAudience());
            ResponsePOJOInstaContentIdeaWRAPPER contentIdea = generateCoreIdea;

            storageService.storeContentIdea(sessionId,contentIdea.getContentIdeas().get(0).getContentIdea());
            Map<String, ResponsePOJOInstaContentIdeaWRAPPER> response = new HashMap<>();
            response.put("contentIdeaData", generateCoreIdea);
            //response.put("sessionId", sessionId);
            System.out.println("session id ye h ->"+sessionId);
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Session-ID", sessionId);
            return  new ResponseEntity<>(response,headers,HttpStatus.OK);

        } catch (Exception e) {
            System.out.println("This is the exception"+e.getMessage());
            Map<String, ResponsePOJOInstaContentIdeaWRAPPER> response = new HashMap<>();
            ResponsePOJOInstaContentIdea error = new ResponsePOJOInstaContentIdea();
            error.setContentIdea("Error: Unable to generate content ideas. Please try again later.");

            ResponsePOJOInstaContentIdeaWRAPPER errorList = new ResponsePOJOInstaContentIdeaWRAPPER();
            errorList.setContentIdeas((List<ResponsePOJOInstaContentIdea>) error);
            response.put("ERROR",  errorList);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/insta_two")
    private ResponseEntity<Map<String, ResponsePOJOInstaCaptionWRAPPER>> generateCaption(@RequestParam String sessionId,@RequestBody CombinedInstaDTO request) {

        //we are doing the LangChain stuff in the service section
        String contentIdea = storageService.getContentIdea(sessionId);
        System.out.println("content idea ye h ->"+contentIdea);
        if (contentIdea == null) {
            Map<String, ResponsePOJOInstaCaptionWRAPPER> errorForSession = new HashMap<>();
            ResponsePOJOInstaCaption error = new ResponsePOJOInstaCaption();
            error.setCaption("Error: Session expired or invalid");
            System.out.println("content idea ye h ->"+contentIdea);

            ResponsePOJOInstaCaptionWRAPPER errorList = new ResponsePOJOInstaCaptionWRAPPER();
            errorList.setCaptions((List<ResponsePOJOInstaCaption>) error);
            errorForSession.put("sessionError",errorList);
            return  new ResponseEntity<>(errorForSession, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            ResponsePOJOInstaCaptionWRAPPER generateCaptionWithCTA = langChainService.generateCaption(request.getNiche()
                    , request.getToneStyle(), request.getCallToAction());
            Map<String, ResponsePOJOInstaCaptionWRAPPER> response = new HashMap<>();
            response.put("captionDATA", generateCaptionWithCTA);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e){
            System.out.println("This is the exception"+e.getMessage());
            Map<String, ResponsePOJOInstaCaptionWRAPPER> response = new HashMap<>();
            ResponsePOJOInstaCaption error = new ResponsePOJOInstaCaption();
            error.setCaption("Error: Unable to generate content ideas. Please try again later.");

            ResponsePOJOInstaCaptionWRAPPER errorList = new ResponsePOJOInstaCaptionWRAPPER();
            errorList.setCaptions((List<ResponsePOJOInstaCaption>) error);
            response.put("ERROR",  errorList);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // Returning a JSON response  not using the instaService currently will integrate when we integrate Langchain AI
    }

    @PostMapping("/insta_three")
    private ResponseEntity<Map<String, ResponsePOJOInstaHashtagsWRAPPER>> generateHashtags(@RequestBody CombinedInstaDTO request) {
        List<String> generatedHashtags = new ArrayList<>();

        //we are doing the LangChain stuff in the service section
        try {
            ResponsePOJOInstaHashtagsWRAPPER generatedHashtags1 = langChainService.generateHashtags(request.getNiche(), request.getKeywords(),
                    request.getTrendingOrEvergreen());
            Map<String, ResponsePOJOInstaHashtagsWRAPPER> response = new HashMap<>();
            response.put("hashtagsDATA", generatedHashtags1);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e){
            System.out.println("This is the exception"+e.getMessage());
            Map<String, ResponsePOJOInstaHashtagsWRAPPER> response = new HashMap<>();
            ResponsePOJOInstaHashtags error = new ResponsePOJOInstaHashtags();
            error.setHashtag("Error: Unable to generate content ideas. Please try again later.");

            ResponsePOJOInstaHashtagsWRAPPER errorList = new ResponsePOJOInstaHashtagsWRAPPER();
            errorList.setHashtags((List<ResponsePOJOInstaHashtags>) error);

            response.put("ERROR",  errorList);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // Returning a JSON response / not using the instaService currently will integrate when we integrate Langchain AI
    }


    @PostMapping("/insta_four")
    private ResponseEntity<Map<String, ResponsePOJOInstaDesignAndAestheticWRAPPER>> generateDesignAndSuggestion(@RequestBody CombinedInstaDTO request) {

        //we are doing the LangChain stuff in the service section
        try {
            ResponsePOJOInstaDesignAndAestheticWRAPPER generateDesignAestheticSugestion = langChainService.generateDesignAndAesthetic(request.getNiche(),
                    request.getToneStyle(), request.getContentType());
            Map<String, ResponsePOJOInstaDesignAndAestheticWRAPPER> response = new HashMap<>();
            response.put("hookDATA", generateDesignAestheticSugestion);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e) {
            System.out.println("This is the exception"+e.getMessage());
            // Returning a JSON response not using the instaService currently will integrate when we integrate Langchain AI
            Map<String, ResponsePOJOInstaDesignAndAestheticWRAPPER> response = new HashMap<>();
            ResponsePOJOInstaDesignAndAesthetic error = new ResponsePOJOInstaDesignAndAesthetic();
            error.setDesignTip("Error: Unable to generate content ideas. Please try again later.");

            ResponsePOJOInstaDesignAndAestheticWRAPPER errorList = new ResponsePOJOInstaDesignAndAestheticWRAPPER();
            errorList.setDesignTips((List<ResponsePOJOInstaDesignAndAesthetic>) error);
            response.put("ERROR",  errorList);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/insta_five")
    private ResponseEntity<Map<String, ResponsePOJOInstaEngagementStrategiesWRAPPER>> generateStrategyForEngagement(@RequestBody CombinedInstaDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            ResponsePOJOInstaEngagementStrategiesWRAPPER generateEngagementTips = langChainService.generateEngagementStrategies(request.getContentGoal(), request.getTargetAudience());
            Map<String, ResponsePOJOInstaEngagementStrategiesWRAPPER> response = new HashMap<>();
            response.put("strategyDATA", generateEngagementTips);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        // Returning a JSON response
        catch (Exception e) {
            System.out.println("This is the exception"+e.getMessage());
            Map<String, ResponsePOJOInstaEngagementStrategiesWRAPPER> response = new HashMap<>();
            ResponsePOJOInstaEngagementStrategies error = new ResponsePOJOInstaEngagementStrategies();
            error.setHook("Error: Unable to generate content ideas. Please try again later.");

            ResponsePOJOInstaEngagementStrategiesWRAPPER errorList = new ResponsePOJOInstaEngagementStrategiesWRAPPER();
            errorList.setEngagementStrategies((List<ResponsePOJOInstaEngagementStrategies>) error);
            response.put("ERROR",  errorList);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/insta_six")
    private ResponseEntity<Map<String, ResponsePOJOInstaBestPostingTimeWRAPPER>> generateContentPostingTime(@RequestBody CombinedInstaDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            ResponsePOJOInstaBestPostingTimeWRAPPER generateBestPostingTime = langChainService.suggestBestPostTime( request.getTargetAudience(),request.getNiche());
            Map<String, ResponsePOJOInstaBestPostingTimeWRAPPER> response = new HashMap<>();
            response.put("contentAdviceDATA", generateBestPostingTime);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        // Returning a JSON response
        catch (Exception e){
            System.out.println("This is the exception"+e.getMessage());
            Map<String, ResponsePOJOInstaBestPostingTimeWRAPPER> response = new HashMap<>();
            ResponsePOJOInstaBestPostingTime error = new ResponsePOJOInstaBestPostingTime();
            error.setBestTime("Error: Unable to generate content ideas. Please try again later.");

            ResponsePOJOInstaBestPostingTimeWRAPPER errorList = new ResponsePOJOInstaBestPostingTimeWRAPPER();
            errorList.setBestPostTimes((List<ResponsePOJOInstaBestPostingTime>) error);
            response.put("ERROR",  errorList);
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/insta_chain")
    private ResponseEntity<Map<String, ResponsePOJOChainIdealSuggestion>> generateContentChainedVersion(@RequestBody CombinedInstaDTO request)
    {
        ResponsePOJOChainIdealSuggestion responsePOJOChainIdealSuggestion = new ResponsePOJOChainIdealSuggestion();
        try {
            ResponsePOJOInstaContentIdeaWRAPPER  contentIdea = langChainService.generateContentIdeas(
                    request.getContentGoal(),
                    request.getNiche(),
                    request.getContentType(),
                    request.getTrendingOrEvergreen(),
                    request.getTargetAudience()
            );
            System.out.println("Generated Content Idea: " + contentIdea);

            //ResponsePOJOInstaContentIdea  idea = contentIdea.getContentIdeas().get(0); // Take the first content idea

            ResponsePOJOInstaContentIdea idea = contentIdea.getContentIdeas().get(0);
                // Step 2 generate caption based on the content idea
            ResponsePOJOInstaCaptionWRAPPER caption = langChainService.generateCaption(
                        idea.getContentIdea(),
                        request.getToneStyle(),
                        request.getCallToAction()
                );

                // Step 3: Generate Hashtags based on inputs
            ResponsePOJOInstaHashtagsWRAPPER hashtags = langChainService.generateHashtags(
                        request.getNiche(),
                        request.getKeywords(),
                        request.getTrendingOrEvergreen()
                );
                // Step 4 generate Design And Aesthetic

            ResponsePOJOInstaDesignAndAestheticWRAPPER dAndA = langChainService.generateDesignAndAesthetic(
                        request.getNiche(),
                        request.getToneStyle(),
                        request.getContentGoal()
                );

                //Step 5 generate Engagement Strategies

            ResponsePOJOInstaEngagementStrategiesWRAPPER EngagementStrategies = langChainService.generateEngagementStrategies(
                        request.getContentGoal(),
                        request.getTargetAudience()
                );

                //Step 6 generate time to post

            ResponsePOJOInstaBestPostingTimeWRAPPER TimeToPost = langChainService.suggestBestPostTime(
                        request.getTargetAudience(),
                        request.getNiche()
                );
                responsePOJOChainIdealSuggestion.setContentIdea(idea.getContentIdea());
                responsePOJOChainIdealSuggestion.setScript(idea.getScript());
                responsePOJOChainIdealSuggestion.setWhyThisWorks(idea.getWhyThisWorks());
                responsePOJOChainIdealSuggestion.setCaption(caption.getCaptions().get(0).getCaption());
                responsePOJOChainIdealSuggestion.setHashtags(hashtags.getHashtags().get(0).getHashtag());
                responsePOJOChainIdealSuggestion.setDesignAndAesthetic(dAndA.getDesignTips().get(0).getDesignTip());
                responsePOJOChainIdealSuggestion.setEngagementStrategies(EngagementStrategies.getEngagementStrategies().get(0).getHook());
                responsePOJOChainIdealSuggestion.setTimeToPost(TimeToPost.getBestPostTimes().get(0).getBestTime());
               // return responsePOJOChainIdealSuggestion;

            Map<String, ResponsePOJOChainIdealSuggestion> responsereturned = new HashMap<>();
            responsereturned.put("contentAdvice", responsePOJOChainIdealSuggestion);
            return new ResponseEntity<>(responsereturned, HttpStatus.OK);

        }
        // Returning a JSON response
        catch (Exception e){
            System.out.println("This is the exception"+e.getMessage());
            Map<String, ResponsePOJOChainIdealSuggestion> responsereturnedError = new HashMap<>();
            responsePOJOChainIdealSuggestion.setContentIdea("Error: Unable to generate content ideas. Please try again later.");
            responsereturnedError.put("ERROR", responsePOJOChainIdealSuggestion);
            return  new ResponseEntity<>(responsereturnedError, HttpStatus.INTERNAL_SERVER_ERROR);
        }

      }


}
