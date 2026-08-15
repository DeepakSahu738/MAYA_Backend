package com.MAYA.MAYA.Config;

import com.MAYA.MAYA.Service.ai.AnalyticsTools;
import com.MAYA.MAYA.Service.ai.CommentTools;
import com.MAYA.MAYA.Service.ai.MayaAiService;
import com.MAYA.MAYA.Service.ai.ScheduleTools;
import com.MAYA.MAYA.Service.ai.StrategyTools;
import com.MAYA.MAYA.Service.ai.TrendTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the Maya AI orchestrator.
 * 
 * What this does:
 * 1. Creates a streaming OpenAI model (for SSE responses)
 * 2. Registers the AnalyticsTools so the LLM can call them
 * 3. Adds chat memory (last 20 messages per session — not persistent)
 * 4. Builds the MayaAiService that the controller will use
 *
 * To add new tools later:
 *   .tools(analyticsTools, contentTools, commentTools)
 * That's it — the LLM auto-discovers the new @Tool methods.
 */
@Configuration
public class MayaAiConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Bean
    public StreamingChatLanguageModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
            .apiKey(openAiApiKey)
            .modelName("gpt-4o-mini")
            .temperature(0.7)
            .build();
    }

    @Bean
    public MayaAiService mayaAiService(
            StreamingChatLanguageModel streamingModel,
            AnalyticsTools analyticsTools,
            ScheduleTools scheduleTools,
            CommentTools commentTools,
            TrendTools trendTools,
            StrategyTools strategyTools) {

        return AiServices.builder(MayaAiService.class)
            .streamingChatLanguageModel(streamingModel)
            .tools(analyticsTools, scheduleTools, commentTools, trendTools, strategyTools)
            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
            .build();
    }
}
