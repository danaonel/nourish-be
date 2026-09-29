package app.nourish.web;

import app.nourish.domain.NotificationPrefs;
import app.nourish.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping("/prefs")
    public NotificationPrefs get(HttpServletRequest req) {
        return notifications.get(currentUser.resolve(req));
    }

    /**
     * @param recoveryAck explicit acknowledgment required for a RECOVERY user to
     *        enable calorie-budget notifications (SG-2).
     */
    @PutMapping("/prefs")
    public NotificationPrefs update(HttpServletRequest req,
                                    @RequestParam(name = "recoveryAck", defaultValue = "false") boolean recoveryAck,
                                    @RequestBody NotificationPrefs prefs) {
        return notifications.update(currentUser.resolve(req), prefs, recoveryAck);
    }

    /** NFR-3: store the Web Push (VAPID) subscription. */
    @PostMapping("/subscribe")
    public NotificationPrefs subscribe(HttpServletRequest req, @RequestBody Map<String, String> body) {
        return notifications.saveSubscription(currentUser.resolve(req),
                body.get("endpoint"), body.get("p256dh"), body.get("auth"));
    }
}
