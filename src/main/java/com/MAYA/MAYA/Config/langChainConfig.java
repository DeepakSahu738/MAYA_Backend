package com.MAYA.MAYA.Config;
import com.MAYA.MAYA.Service.DemoLangChainServiceImpl;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.Value;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

@Configuration
public class langChainConfig {
//    @Bean
//    ChatMemoryProvider chatMemoryProvider(Tokenizer tokenizer) {
//        return chatId -> TokenWindowChatMemory.withMaxTokens(1000, tokenizer);
//    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

//    @Bean
//    public AiService aiService() {
//        return new DemoLangChainServiceImpl(); // Replace with actual implementation
//    }


//    @Bean
//    ChatMemory chatMemory() {
//        return MessageWindowChatMemory.withMaxMessages(10);
//    }
    @Bean
    public ChatModelListener chatModelListener() {
        return new ChatModelListener() {
            public void onMessage(String message) {
                System.out.println("Message received: " + message);
            }
        };
    }



}
