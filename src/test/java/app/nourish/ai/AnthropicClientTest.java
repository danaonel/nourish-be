package app.nourish.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the Messages API request shape and response parsing without making a
 * network call.
 */
class AnthropicClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void notConfiguredWithoutKey() {
        AnthropicClient client = new AnthropicClient("", "claude-3-5-sonnet-latest");
        assertFalse(client.isConfigured());
        assertTrue(client.complete("sys", "hi", 100).isEmpty(),
                "must return empty (fall back) when no key is set");
    }

    @Test
    void buildsValidMessagesRequest() throws Exception {
        AnthropicClient client = new AnthropicClient("test-key", "claude-3-5-sonnet-latest");
        String body = client.buildRequestBody("You are a coach", "Plan my week", 512);
        JsonNode root = mapper.readTree(body);

        assertEquals("claude-3-5-sonnet-latest", root.path("model").asText());
        assertEquals(512, root.path("max_tokens").asInt());
        assertEquals("You are a coach", root.path("system").asText());
        assertTrue(root.path("messages").isArray());
        assertEquals("user", root.path("messages").get(0).path("role").asText());
        assertEquals("Plan my week", root.path("messages").get(0).path("content").asText());
    }

    @Test
    void parsesTextFromResponseContentBlocks() {
        AnthropicClient client = new AnthropicClient("test-key", "m");
        String sample = """
                {"id":"msg_1","type":"message","role":"assistant",
                 "content":[{"type":"text","text":"Hello "},{"type":"text","text":"world"}]}
                """;
        Optional<String> text = client.extractText(sample);
        assertTrue(text.isPresent());
        assertEquals("Hello world", text.get());
    }

    @Test
    void emptyContentYieldsEmpty() {
        AnthropicClient client = new AnthropicClient("test-key", "m");
        assertTrue(client.extractText("{\"content\":[]}").isEmpty());
    }
}
