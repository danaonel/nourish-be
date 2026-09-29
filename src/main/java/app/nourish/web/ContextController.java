package app.nourish.web;

import app.nourish.domain.Profile;
import app.nourish.service.ContextService;
import app.nourish.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/context")
public class ContextController {

    private final ContextService context;
    private final ProfileService profiles;
    private final CurrentUser currentUser;

    public ContextController(ContextService context, ProfileService profiles, CurrentUser currentUser) {
        this.context = context;
        this.profiles = profiles;
        this.currentUser = currentUser;
    }

    public record MenuRequest(String venue, List<ContextService.MenuItem> menu) {}

    /** FR-14/15/16: "I'm at X" restaurant guidance with trigger suppression. */
    @PostMapping("/menu-guidance")
    public ContextService.MenuGuidance menuGuidance(HttpServletRequest req, @RequestBody MenuRequest body) {
        Profile p = profiles.getOrCreate(currentUser.resolve(req));
        return context.guide(p, body.venue(), body.menu() == null ? List.of() : body.menu());
    }
}
