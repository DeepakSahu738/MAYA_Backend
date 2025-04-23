package com.MAYA.MAYA.Service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service  // Registers this class as a Spring Bean
public class DemoLangChainServiceImpl{ //implements LangChainAiServiceInstagram {
    public DemoLangChainServiceImpl(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

//    @Override
//    public String chat(String userMessage) {
//        return "";
//    }  // Implementation

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    public String generateResponse(String prompt) {
        return chatLanguageModel.generate(prompt);
    }

}

