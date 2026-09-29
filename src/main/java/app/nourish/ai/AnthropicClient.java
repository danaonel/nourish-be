package app.nourish.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Thin client for the Anthropic Messages API
 * (POST https://api.anthropic.com/v1/messages).
 *
 * <p>Built on the JDK {@link HttpClient} so no extra dependency is needed. Every
 * call returns an {@link Optional} — callers must fall back gracefully (to the
 * deterministic local logic) whenever this is empty, which happens when no API
 * key is configured, the request fails, or the response can't be parsed. The LLM
 * is never a hard dependency of a request path.</p>
 */
@Component
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);
    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnthropicClient(
            @Value("${nourish.ai.anthropic-api-key:}") String apiKey,
            @Value("${nourish.ai.model:claude-3-5-sonnet-latest}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    /**
     * Sends a single-turn request (a system prompt + one user message) and returns
     * the concatenated text of the assistant's reply, or empty on any failure.
     *
     * @param maxTokens upper bound on the response size
     */
    public Optional<String> complete(String systemPrompt, String userMessage, int maxTokens) {
        if (!isConfigured()) return Optional.empty();
        try {
            String body = buildRequestBody(systemPrompt, userMessage, maxTokens);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(45))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Anthropic API returned status {}: {}", response.statusCode(),
                        truncate(response.body()));
                return Optional.empty();
            }
            return extractText(response.body());
        } catch (Exception e) {
            // Network error, timeout, interrupt, parse error — always fall back.
            log.warn("Anthropic API call failed, falling back to local logic: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Builds the Messages API request JSON safely (Jackson handles escaping). */
    String buildRequestBody(String systemPrompt, String userMessage, int maxTokens) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            root.put("system", systemPrompt);
        }
        ArrayNode messages = root.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        return root.toString();
    }

    /** Concatenates all text blocks from the assistant response's content array. */
    Optional<String> extractText(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) return Optional.empty();
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    sb.append(block.path("text").asText());
                }
            }
            String text = sb.toString().trim();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            log.warn("Could not parse Anthropic response: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
