package app.nourish.web;

import app.nourish.domain.Profile;
import app.nourish.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;
    private final CurrentUser currentUser;

    public ProfileController(ProfileService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Profile get(HttpServletRequest req) {
        return service.getOrCreate(currentUser.resolve(req));
    }

    @PutMapping
    public Profile save(HttpServletRequest req, @RequestBody Profile profile) {
        return service.save(currentUser.resolve(req), profile);
    }
}
