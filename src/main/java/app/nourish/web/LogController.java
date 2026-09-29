package app.nourish.web;

import app.nourish.ai.SafetyService;
import app.nourish.domain.LogEntry;
import app.nourish.domain.Profile;
import app.nourish.service.LoggingService;
import app.nourish.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LoggingService logs;
    private final ProfileService profiles;
    private final SafetyService safety;
    private final CurrentUser currentUser;

    public LogController(LoggingService logs, ProfileService profiles,
                         SafetyService safety, CurrentUser currentUser) {
        this.logs = logs;
        this.profiles = profiles;
        this.safety = safety;
        this.currentUser = currentUser;
    }

    /** FR-18: quick-add. Accepts a clientId for idempotent offline sync. */
    @PostMapping
    public Map<String, Object> add(HttpServletRequest req, @RequestBody LogEntry entry) {
        String userId = currentUser.resolve(req);
        LogEntry saved = logs.add(userId, entry);

        // SG-3: evaluate for concerning patterns on each food log; return a support
        // nudge alongside the saved entry rather than blocking anything.
        Profile p = profiles.getOrCreate(userId);
        SafetyService.Concern concern = safety.evaluate(p, logs.recent(userId, Duration.ofDays(1)));
        return Map.of("entry", saved, "concern", concern);
    }

    /** Bulk endpoint for the offline queue to flush on reconnect (NFR-2). */
    @PostMapping("/batch")
    public List<LogEntry> addBatch(HttpServletRequest req, @RequestBody List<LogEntry> entries) {
        String userId = currentUser.resolve(req);
        return entries.stream().map(e -> logs.add(userId, e)).toList();
    }

    @GetMapping
    public List<LogEntry> list(HttpServletRequest req,
                               @RequestParam(required = false) LogEntry.Type type) {
        return logs.list(currentUser.resolve(req), type);
    }

    /** FR-20: trend rollups. */
    @GetMapping("/trends")
    public Map<String, Object> trends(HttpServletRequest req,
                                      @RequestParam(defaultValue = "30") int days) {
        return logs.trends(currentUser.resolve(req), days);
    }
}
