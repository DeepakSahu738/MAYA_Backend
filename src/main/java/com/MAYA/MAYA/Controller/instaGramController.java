package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.instagram.*;
import com.MAYA.MAYA.Service.TemporaryStorageService;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceInstagram;
import com.MAYA.MAYA.Service.contentServices.instaGramService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
            return  new ResponseEntity<>(response, HttpStatus.OK);

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
    private ResponseEntity<Map<String, String>> generateCaption(@RequestParam String sessionId,@RequestBody CombinedInstaDTO request) {

        //we are doing the LangChain stuff in the service section
        String contentIdea = storageService.getContentIdea(sessionId);
        if (contentIdea == null) {
            Map<String, String> errorForSession = new HashMap<>();
            errorForSession.put("sessionError", "Session expired or invalid");
            return  new ResponseEntity<>(errorForSession, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            String generateCaptionWithCTA = instaGramService.generateCaptionWithCTA(contentIdea
                    , request.getToneStyle(), request.getCallToAction());
            Map<String, String> response = new HashMap<>();
            response.put("caption", generateCaptionWithCTA);
            return  new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e){
            System.out.println("This is the exception"+e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // Returning a JSON response  not using the instaService currently will integrate when we integrate Langchain AI
    }

    @PostMapping("/insta_three")
    private ResponseEntity<Map<String, String>> generateHashtags(@RequestBody CombinedInstaDTO request) {
        List<String> generatedHashtags = new ArrayList<>();

        //we are doing the LangChain stuff in the service section
        try {
            String generatedHashtags1 = instaGramService.suggestHashtags(request.getNiche(), request.getKeywords(),
                    request.getTrendingOrEvergreen());
            Map<String, String> response = new HashMap<>();
            response.put("hashtags", generatedHashtags1);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e){
            System.out.println("This is the exception"+e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", " Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // Returning a JSON response / not using the instaService currently will integrate when we integrate Langchain AI
    }


    @PostMapping("/insta_four")
    private ResponseEntity<Map<String, String>> generateDesignAndSuggestion(@RequestBody CombinedInstaDTO request) {

        //we are doing the LangChain stuff in the service section
        try {
            String generateDesignAestheticSugestion = instaGramService.suggestDesignAndAesthetic(request.getNiche(),
                    request.getToneStyle(), request.getContentType());
            Map<String, String> response = new HashMap<>();
            response.put("hook", generateDesignAestheticSugestion);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception e) {
            System.out.println("This is the exception"+e.getMessage());
            // Returning a JSON response not using the instaService currently will integrate when we integrate Langchain AI
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/insta_five")
    private ResponseEntity<Map<String, String>> generateStrategyForEngagement(@RequestBody CombinedInstaDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateEngagementTips = instaGramService.generateEngagementStrategies(request.getContentGoal(), request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("strategy", generateEngagementTips);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        // Returning a JSON response
        catch (Exception e) {
            System.out.println("This is the exception"+e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
            return  new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/insta_six")
    private ResponseEntity<Map<String, String>> generateContentPostingTime(@RequestBody CombinedInstaDTO request) {
        //we are doing the LangChain stuff in the service section
        try {
            String generateBestPostingTime = instaGramService.suggestBestPostTime(request.getNiche(), request.getTargetAudience());
            Map<String, String> response = new HashMap<>();
            response.put("contentAdvice", generateBestPostingTime);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        // Returning a JSON response
        catch (Exception e){
            System.out.println("This is the exception"+e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("ERROR", "Error: Unable to generate content ideas. Please try again later.");
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
                String caption = langChainService.generateCaption(
                        idea.getContentIdea(),
                        request.getToneStyle(),
                        request.getCallToAction()
                );

                // Step 3: Generate Hashtags based on inputs
                String hashtags = langChainService.generateHashtags(idea.getContentIdea(),
                        request.getNiche(),
                        request.getKeywords(),
                        request.getTrendingOrEvergreen()
                );
                // Step 4 generate Design And Aesthetic

                String dAndA = langChainService.generateDesignAndAesthetic(idea.getContentIdea(),
                        request.getNiche(),
                        request.getToneStyle(),
                        request.getContentGoal()
                );

                //Step 5 generate Engagement Strategies

                String EngagementStrategies = langChainService.generateEngagementStrategies(idea.getContentIdea(),
                        request.getContentGoal(),
                        request.getTargetAudience()
                );

                //Step 6 generate time to post

                String TimeToPost = langChainService.suggestBestPostTime(idea.getContentIdea(),
                        request.getTargetAudience(),
                        request.getNiche()
                );
                responsePOJOChainIdealSuggestion.setContentIdea(idea.getContentIdea());
                responsePOJOChainIdealSuggestion.setScript(idea.getScript());
                responsePOJOChainIdealSuggestion.setWhyThisWorks(idea.getWhyThisWorks());
                responsePOJOChainIdealSuggestion.setCaption(caption);
                responsePOJOChainIdealSuggestion.setHashtags(hashtags);
                responsePOJOChainIdealSuggestion.setDesignAndAesthetic(dAndA);
                responsePOJOChainIdealSuggestion.setEngagementStrategies(EngagementStrategies);
                responsePOJOChainIdealSuggestion.setTimeToPost(TimeToPost);
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
