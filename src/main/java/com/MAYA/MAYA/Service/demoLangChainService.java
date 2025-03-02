package com.MAYA.MAYA.Service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@AiService
 public interface demoLangChainService {

    @SystemMessage("You are a polite assistant")
    String chat(String userMessage);
}