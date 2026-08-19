package com.MAYA.MAYA.Service.phyllo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Communicates with Phyllo API.
 *
 * This is the ONLY class that knows Phyllo API details.
 * All other services work with Maya entities — never Phyllo directly.
 *
 * Phyllo API uses Basic Auth: base64(client_id:client_secret)
 *
 * Key endpoints used:
 * - POST /v1/users          → Create a Phyllo user (one per Maya user)
 * - POST /v1/sdk-tokens     → Generate token for frontend Connect SDK
 * - GET  /v1/social/contents → Fetch posts for a connected account
 * - GET  /v1/social/comments → Fetch comments for a connected account
 * - GET  /v1/profiles        → Fetch profile info for a connected account
 */
@Service
@Slf4j
public class PhylloService {

    @Value("${phyllo.base-url}")
    private String baseUrl;

    @Value("${phyllo.client-id}")
    private String clientId;

    @Value("${phyllo.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Step 1: Create a Phyllo user.
     * Call this when a Maya user wants to connect their first social account.
     * One Phyllo user per Maya user — reuse if already created.
     *
     * If user already exists on Phyllo (same external_id), fetches existing user instead.
     *
     * @param mayaUserId - your internal user ID (stored as external_id in Phyllo)
     * @param userName   - display name
     * @return Phyllo user ID (UUID string)
     */
    public String createPhylloUser(Long mayaUserId, String userName) {
        String url = baseUrl + "/v1/users";

        Map<String, Object> body = new HashMap<>();
        body.put("name", userName);
        body.put("external_id", mayaUserId.toString());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            String phylloUserId = json.get("id").asText();
            log.info("Created Phyllo user: {} for Maya user: {}", phylloUserId, mayaUserId);
            return phylloUserId;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // If user already exists, fetch by external_id
            if (e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains("user_exists_with_external_id")) {
                log.info("Phyllo user already exists for Maya user: {} — fetching existing", mayaUserId);
                return fetchExistingPhylloUser(mayaUserId);
            }
            log.error("Failed to create Phyllo user for Maya user: {}", mayaUserId, e);
            throw new RuntimeException("Failed to create Phyllo user: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create Phyllo user for Maya user: {}", mayaUserId, e);
            throw new RuntimeException("Failed to create Phyllo user: " + e.getMessage());
        }
    }

    /**
     * Fetch existing Phyllo user by external_id (Maya user ID).
     */
    private String fetchExistingPhylloUser(Long mayaUserId) {
        String url = baseUrl + "/v1/users?external_id=" + mayaUserId;

        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode data = json.has("data") ? json.get("data") : json;

            if (data.isArray() && data.size() > 0) {
                return data.get(0).get("id").asText();
            } else if (data.has("id")) {
                return data.get("id").asText();
            }

            throw new RuntimeException("Could not find existing Phyllo user for external_id: " + mayaUserId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch existing Phyllo user: " + e.getMessage());
        }
    }

    /**
     * Step 2: Generate an SDK token for the frontend Connect widget.
     * Frontend uses this token to open the Phyllo Connect SDK.
     *
     * @param phylloUserId - the Phyllo user ID from step 1
     * @return SDK token string
     */
    public String generateSdkToken(String phylloUserId) {
        String url = baseUrl + "/v1/sdk-tokens";

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", phylloUserId);
        body.put("products", List.of("IDENTITY", "ENGAGEMENT"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            String sdkToken = json.get("sdk_token").asText();
            log.info("Generated SDK token for Phyllo user: {}", phylloUserId);
            return sdkToken;
        } catch (Exception e) {
            log.error("Failed to generate SDK token for Phyllo user: {}", phylloUserId, e);
            throw new RuntimeException("Failed to generate SDK token: " + e.getMessage());
        }
    }

    /**
     * Fetch account details from Phyllo after connection.
     *
     * @param accountId - the Phyllo account ID from the SDK callback
     * @return JsonNode with account info (platform, username, etc.)
     */
    public JsonNode getAccountDetails(String accountId) {
        String url = baseUrl + "/v1/accounts/" + accountId;

        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch account details for: {}", accountId, e);
            throw new RuntimeException("Failed to fetch account details: " + e.getMessage());
        }
    }

    /**
     * Fetch social contents (posts) for a connected account.
     *
     * @param accountId - the Phyllo account ID
     * @param limit     - number of posts to fetch (max 100 per page)
     * @return JsonNode containing the posts data array
     */
    public JsonNode fetchContents(String accountId, int limit) {
        return fetchContents(accountId, limit, 0);
    }

    /**
     * Fetch social contents (posts) for a connected account with offset pagination.
     *
     * @param accountId - the Phyllo account ID
     * @param limit     - number of posts to fetch per page (max 100)
     * @param offset    - offset for pagination (0-based)
     * @return JsonNode containing the posts data array and metadata
     */
    public JsonNode fetchContents(String accountId, int limit, int offset) {
        String url = baseUrl + "/v1/social/contents?account_id=" + accountId + "&limit=" + limit + "&offset=" + offset;

        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch contents for account: {} (offset: {})", accountId, offset, e);
            throw new RuntimeException("Failed to fetch contents: " + e.getMessage());
        }
    }

    /**
     * Fetch comments for a specific content item (post).
     * Phyllo requires BOTH account_id AND content_id for comments.
     *
     * @param accountId - the Phyllo account ID
     * @param contentId - the Phyllo content ID (post ID)
     * @param limit     - number of comments to fetch
     * @return JsonNode containing the comments data array
     */
    public JsonNode fetchComments(String accountId, String contentId, int limit) {
        String url = baseUrl + "/v1/social/comments?account_id=" + accountId + "&content_id=" + contentId + "&limit=" + limit;

        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch comments for content: {}", contentId, e);
            throw new RuntimeException("Failed to fetch comments: " + e.getMessage());
        }
    }

    /**
     * Request historic data from Phyllo (data beyond last 90 days).
     * This is async on Phyllo's side — they process it and make it available later.
     * Call fetchContents() again after this completes to get older data.
     *
     * Endpoint: POST /v1/social/contents/fetch-historic
     * Body: { "account_id": "...", "from_date": "2019-08-24" }
     *
     * @param accountId - the Phyllo account ID
     * @return true if request was accepted, false if failed or not supported
     */
    public boolean requestHistoricData(String accountId) {
        String url = baseUrl + "/v1/social/contents/fetch-historic";

        Map<String, Object> body = new HashMap<>();
        body.put("account_id", accountId);
        body.put("from_date", "2019-01-01"); // fetch as far back as possible

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("Historic data requested for account: {} — status: {}", accountId, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 400/404 means not supported or already requested — not a hard failure
            log.warn("Historic data request failed for account {}: {} — {}", accountId, e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Historic data request failed for account {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Fetch profile info for a connected account.
     *
     * @param accountId - the Phyllo account ID
     * @return JsonNode containing profile data
     */
    public JsonNode fetchProfile(String accountId) {
        String url = baseUrl + "/v1/profiles?account_id=" + accountId;

        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch profile for account: {}", accountId, e);
            throw new RuntimeException("Failed to fetch profile: " + e.getMessage());
        }
    }

    // --- Private: Build Basic Auth headers ---

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedCredentials);

        return headers;
    }
}
