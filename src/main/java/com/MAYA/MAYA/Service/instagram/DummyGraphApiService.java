package com.MAYA.MAYA.Service.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyGraphApiService {
    
    private final ObjectMapper objectMapper;
    
    private static final String[] PROFILE_FILES = {
        "phyllo_fitlife_by_meera.json"
    };
    
    private static final String[] COMMENT_FILES = {
        "phyllo_comments_fitlife_by_meera.json"
    };
    
    /**
     * Loads all 4 creator post data files (Phyllo content format).
     * Each file contains a "data" array of posts for one creator.
     */
    public List<JsonNode> loadAllPostData() {
        List<JsonNode> results = new ArrayList<>();
        for (String file : PROFILE_FILES) {
            JsonNode node = loadResource("profile_data/" + file);
            if (node != null) {
                results.add(node);
            }
        }
        return results;
    }
    
    /**
     * Loads all 4 comment data files (Phyllo comments format).
     * Each file contains a "data" array of comments for one creator.
     */
    public List<JsonNode> loadAllCommentData() {
        List<JsonNode> results = new ArrayList<>();
        for (String file : COMMENT_FILES) {
            JsonNode node = loadResource("profiles_commentList/" + file);
            if (node != null) {
                results.add(node);
            }
        }
        return results;
    }
    
    /**
     * Loads a single post data file by profile name.
     */
    public JsonNode loadPostData(String profileName) {
        return loadResource("profile_data/phyllo_" + profileName + ".json");
    }
    
    /**
     * Loads a single comment data file by profile name.
     */
    public JsonNode loadCommentData(String profileName) {
        return loadResource("profiles_commentList/phyllo_comments_" + profileName + ".json");
    }
    
    private JsonNode loadResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return objectMapper.readTree(resource.getInputStream());
        } catch (IOException e) {
            log.error("Failed to load: {}", path, e);
            return null;
        }
    }
}
