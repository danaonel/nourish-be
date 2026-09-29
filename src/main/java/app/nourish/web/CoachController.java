package app.nourish.web;

import app.nourish.ai.CoachingService;
import app.nourish.ai.SafetyService;
import app.nourish.domain.Profile;
import app.nourish.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final CoachingService coaching;
    private final SafetyService safety;
    private final ProfileService profiles;
    private final CurrentUser currentUser;

    public CoachController(CoachingService coaching, SafetyService safety,
                           ProfileService profiles, CurrentUser currentUser) {
        this.coaching = coaching;
        this.safety = safety;
        this.profiles = profiles;
        this.currentUser = currentUser;
    }

    /** FR-25 / FR-17: "coach me through this" craving flow. */
    @PostMapping("/craving")
    public List<CoachingService.CoachStep> craving(HttpServletRequest req,
                                                   @RequestBody(required = false) Map<String, String> body) {
        Profile p = profiles.getOrCreate(currentUser.resolve(req));
        String situation = body == null ? null : body.get("situation");
        return coaching.cravingFlow(p, situation);
    }

    /** FR-13a: pantry-based suggestion. */
    @PostMapping("/pantry")
    public Map<String, String> pantry(HttpServletRequest req, @RequestBody Map<String, List<String>> body) {
        Profile p = profiles.getOrCreate(currentUser.resolve(req));
        return Map.of("suggestion", coaching.pantrySuggestion(p, body.get("onHand")));
    }

    /** SG-3 / SG-5: support resources, available on demand. */
    @GetMapping("/resources")
    public List<SafetyService.Resource> resources() {
        return safety.resources();
    }
}
