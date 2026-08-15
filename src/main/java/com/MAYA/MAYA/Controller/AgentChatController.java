package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.Service.CreatorAccessService;
import com.MAYA.MAYA.Service.RateLimiterService;
import com.MAYA.MAYA.Service.ai.MayaAiService;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Chat endpoint — accepts a message + creatorId, streams back AI response via SSE.
 *
 * How it works:
 * 1. Frontend sends POST with { "message": "What's my engagement rate?", "creatorId": 5 }
 * 2. Controller calls MayaAiService.chat() which returns a TokenStream
 * 3. TokenStream emits tokens one by one as the LLM generates them
 * 4. Each token is pushed to a Flux<String> which Spring streams as SSE events
 * 5. Frontend reads the SSE stream and displays tokens in real-time (typewriter effect)
 *
 * SSE format the frontend receives:
 *   data: Your
 *   data: engagement
 *   data: rate
 *   data: is
 *   data: 15.28%
 *   data: [DONE]
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class AgentChatController {

    private final MayaAiService mayaAiService;
    private final CreatorAccessService creatorAccessService;
    private final RateLimiterService rateLimiterService;

    // Rate limits
    private static final int CHAT_MAX_REQUESTS = 30;        // 30 messages
    private static final long CHAT_WINDOW_MS = 60_000;      // per minute

    /**
     * Stream a chat response for a specific creator.
     * Demo creators (5-8): public. Real creators (9+): requires JWT + ownership.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
        // Access check
        if (!creatorAccessService.canAccess(request.creatorId(), jwt)) {
            Sinks.Many<String> errorSink = Sinks.many().unicast().onBackpressureBuffer();
            errorSink.tryEmitNext("[ERROR] Access denied — you don't own this creator account.");
            errorSink.tryEmitNext("[DONE]");
            errorSink.tryEmitComplete();
            return errorSink.asFlux();
        }

        // Rate limit check (20 requests/minute per session)
        String rateLimitKey = "chat:" + (request.sessionId() != null ? request.sessionId() : "anonymous");
        if (!rateLimiterService.isAllowed(rateLimitKey, CHAT_MAX_REQUESTS, CHAT_WINDOW_MS)) {
            Sinks.Many<String> errorSink = Sinks.many().unicast().onBackpressureBuffer();
            errorSink.tryEmitNext("[ERROR] Rate limit exceeded. Please wait a moment before sending more messages.");
            errorSink.tryEmitNext("[DONE]");
            errorSink.tryEmitComplete();
            return errorSink.asFlux();
        }

        log.info("Chat request for creator {}: {}", request.creatorId(), request.message());

        // Create a sink that we'll push tokens into
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // Call the orchestrator — it decides which tools to use and streams the response
        String currentDate = java.time.LocalDate.now().toString();
        String sessionId = request.sessionId() != null ? request.sessionId() : "default";
        TokenStream tokenStream = mayaAiService.chat(sessionId, request.message(), request.creatorId(), currentDate);

        // Wire the token stream to our SSE sink
        tokenStream
            .onNext(sink::tryEmitNext)           // each token → SSE event
            .onComplete(response -> {
                sink.tryEmitNext("[DONE]");       // signal completion
                sink.tryEmitComplete();
            })
            .onError(error -> {
                log.error("Chat stream error for creator {}", request.creatorId(), error);
                sink.tryEmitNext("[ERROR] " + error.getMessage());
                sink.tryEmitComplete();
            })
            .start();

        return sink.asFlux();
    }

    /**
     * Non-streaming endpoint for testing (returns full response at once).
     * 
     * POST /api/chat/send
     * Body: { "message": "...", "creatorId": 5 }
     * Response: { "response": "..." }
     */
    @PostMapping("/send")
    public Flux<String> chatSend(@RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
        return chatStream(request, jwt);
    }

    // Request DTO
    record ChatRequest(String message, Long creatorId, String sessionId) {}
}
