package com.MAYA.MAYA.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class genAi {

    private final WebClient webClient;

    @Autowired
    public genAi(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent").build();
    }

    public Mono<String> getGenAiResponse()
    {
        return webClient.post()
                .uri("?key=AIzaSyByV3ONsN7jJLaTtpuWx2DVj-ybeZ2B7J0")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(getGenAiBody()))
                .retrieve().bodyToMono(String.class);
    }

    public String getGenAiBody()
    {
        return "{\n" +
                "    \"contents\": [\n" +
                "        {\n" +
                "            \"parts\": [\n" +
                "                {\n" +
                "                    \"text\": \"Tell me story about a cow\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }
}
