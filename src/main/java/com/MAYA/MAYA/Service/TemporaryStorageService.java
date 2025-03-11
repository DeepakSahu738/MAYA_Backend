package com.MAYA.MAYA.Service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class TemporaryStorageService {
    private final ConcurrentHashMap<String, String> contentIdeaStore = new ConcurrentHashMap<>();

    public void storeContentIdea(String sessionId, String contentIdea) {
        contentIdeaStore.put(sessionId, contentIdea);
    }

    public String getContentIdea(String sessionId) {
        return contentIdeaStore.get(sessionId);
    }

    public void removeContentIdea(String sessionId) {
        contentIdeaStore.remove(sessionId);
    }
}
