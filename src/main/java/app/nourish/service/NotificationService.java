package app.nourish.service;

import app.nourish.domain.GoalType;
import app.nourish.domain.NotificationPrefs;
import app.nourish.domain.Profile;
import app.nourish.repo.Store;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Push subscription + per-type opt-in management (FR-24, NFR-3). Enforces SG-2:
 * the calorie-budget notification cannot be enabled for a RECOVERY profile without
 * an explicit acknowledgment flag.
 */
@Service
public class NotificationService {

    private final Store<NotificationPrefs> prefs;
    private final ProfileService profileService;

    public NotificationService(Store<NotificationPrefs> prefs, ProfileService profileService) {
        this.prefs = prefs;
        this.profileService = profileService;
    }

    public NotificationPrefs get(String userId) {
        return prefs.findWhere(p -> userId.equals(p.getUserId())).stream().findFirst()
                .orElseGet(() -> {
                    NotificationPrefs np = new NotificationPrefs();
                    np.setUserId(userId);
                    np.setCalorieBudget(false);
                    return prefs.save(np);
                });
    }

    /**
     * @param recoveryAcknowledged explicit, high-friction acknowledgment required
     *        before a RECOVERY user can enable calorie-budget notifications (SG-2).
     */
    public NotificationPrefs update(String userId, NotificationPrefs incoming, boolean recoveryAcknowledged) {
        NotificationPrefs current = get(userId);
        Optional<Profile> profile = profileService.findByUser(userId);
        boolean recovery = profile.map(p -> p.getGoalType() == GoalType.RECOVERY).orElse(false);

        current.setMealPrepReminder(incoming.isMealPrepReminder());
        current.setHydrationReminder(incoming.isHydrationReminder());
        current.setGroceryDayReminder(incoming.isGroceryDayReminder());
        current.setQuietHoursStart(incoming.getQuietHoursStart());
        current.setQuietHoursEnd(incoming.getQuietHoursEnd());

        if (incoming.isCalorieBudget() && recovery && !recoveryAcknowledged) {
            // SG-2: refuse to silently enable calorie framing for recovery users.
            current.setCalorieBudget(false);
        } else {
            current.setCalorieBudget(incoming.isCalorieBudget());
        }
        return prefs.save(current);
    }

    public NotificationPrefs saveSubscription(String userId, String endpoint, String p256dh, String auth) {
        NotificationPrefs current = get(userId);
        current.setPushEndpoint(endpoint);
        current.setPushP256dh(p256dh);
        current.setPushAuth(auth);
        return prefs.save(current);
    }
}
