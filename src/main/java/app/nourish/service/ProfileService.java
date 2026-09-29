package app.nourish.service;

import app.nourish.domain.GoalType;
import app.nourish.domain.NotificationPrefs;
import app.nourish.domain.Profile;
import app.nourish.repo.Store;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Owns the persona-defining profile (BRD §4.1). Applies persona-based defaults on
 * create: RECOVERY disables calorie/weight logging and calorie notifications (SG-2).
 */
@Service
public class ProfileService {

    private final Store<Profile> profiles;
    private final Store<NotificationPrefs> prefs;

    public ProfileService(Store<Profile> profiles, Store<NotificationPrefs> prefs) {
        this.profiles = profiles;
        this.prefs = prefs;
    }

    public Profile getOrCreate(String userId) {
        return findByUser(userId).orElseGet(() -> {
            Profile p = new Profile();
            p.setUserId(userId);
            return persistWithDefaults(p);
        });
    }

    public Optional<Profile> findByUser(String userId) {
        return profiles.findWhere(p -> userId.equals(p.getUserId())).stream().findFirst();
    }

    public Profile save(String userId, Profile incoming) {
        Optional<Profile> existing = findByUser(userId);
        incoming.setUserId(userId);
        existing.ifPresent(p -> incoming.setId(p.getId()));
        incoming.setUpdatedAt(Instant.now());
        applyPersonaDefaults(incoming);
        Profile saved = profiles.save(incoming);
        ensurePrefs(saved);
        return saved;
    }

    private Profile persistWithDefaults(Profile p) {
        applyPersonaDefaults(p);
        Profile saved = profiles.save(p);
        ensurePrefs(saved);
        return saved;
    }

    /** SG-1 / SG-2: never assume weight loss is the default success metric. */
    private void applyPersonaDefaults(Profile p) {
        if (p.getGoalType() == GoalType.RECOVERY) {
            // Recovery guardrails: these stay user-controlled but default protective.
            p.setCalorieLoggingEnabled(false);
            p.setWeightLoggingEnabled(false);
        }
    }

    private void ensurePrefs(Profile p) {
        NotificationPrefs existing = prefs.findWhere(x -> p.getUserId().equals(x.getUserId()))
                .stream().findFirst().orElse(null);
        if (existing == null) {
            NotificationPrefs np = new NotificationPrefs();
            np.setUserId(p.getUserId());
            // SG-2: calorie-budget notifications off by default, and hard-off for RECOVERY.
            np.setCalorieBudget(false);
            prefs.save(np);
        }
    }
}
