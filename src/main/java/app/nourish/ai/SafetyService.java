package app.nourish.ai;

import app.nourish.domain.GoalType;
import app.nourish.domain.LogEntry;
import app.nourish.domain.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Lightweight, non-diagnostic pattern detection (SG-3). When concerning trends
 * appear it returns supportive resources rather than more diet content. This is a
 * support nudge, not a diagnosis (SG-3, SG-5).
 */
@Service
public class SafetyService {

    public record Concern(boolean flagged, String message, List<Resource> resources) {}
    public record Resource(String label, String contact) {}

    private static final List<Resource> RESOURCES = List.of(
        new Resource("NEDA Helpline (US)", "1-800-931-2237"),
        new Resource("988 Suicide & Crisis Lifeline (US)", "988"),
        new Resource("Talk to your clinician / dietitian / therapist", "")
    );

    /**
     * Flags two lightweight signals: a sharp intake drop and compulsive logging
     * frequency. Deliberately conservative — this only surfaces resources.
     */
    public Concern evaluate(Profile profile, List<LogEntry> recentLogs) {
        long foodLogsLastHour = recentLogs.stream()
                .filter(l -> l.getType() == LogEntry.Type.FOOD)
                .filter(l -> l.getTimestamp() != null
                        && Duration.between(l.getTimestamp(), Instant.now()).toMinutes() <= 60)
                .count();

        boolean compulsiveLogging = foodLogsLastHour >= 12;

        Integer todayCalories = recentLogs.stream()
                .filter(l -> l.getType() == LogEntry.Type.FOOD && l.getCalories() != null)
                .filter(l -> l.getTimestamp() != null
                        && Duration.between(l.getTimestamp(), Instant.now()).toHours() <= 24)
                .map(LogEntry::getCalories)
                .reduce(0, Integer::sum);

        // Only treat an extreme intake drop as a signal when the day is well underway.
        boolean extremeDrop = Instant.now().getEpochSecond() % 86400 > 16 * 3600
                && todayCalories > 0 && todayCalories < 500;

        if (compulsiveLogging || extremeDrop) {
            String msg = profile.getGoalType() == GoalType.RECOVERY
                    ? "We noticed a pattern that can be worth checking in about. You're not alone — here are people who can help."
                    : "We noticed something worth a gentle check-in. If food feels hard right now, here are supportive resources.";
            return new Concern(true, msg, RESOURCES);
        }
        return new Concern(false, null, List.of());
    }

    public List<Resource> resources() {
        return RESOURCES;
    }
}
