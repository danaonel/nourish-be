package app.nourish.web;

import app.nourish.ai.AiOrchestrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Lightweight service metadata for the frontend (e.g. whether the LLM is wired). */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final AiOrchestrationService ai;

    public MetaController(AiOrchestrationService ai) {
        this.ai = ai;
    }

    @GetMapping
    public Map<String, Object> meta() {
        return Map.of(
                "app", "Nourish",
                "version", "1.0.0",
                "llmEnabled", ai.isLlmEnabled()
        );
    }
}
