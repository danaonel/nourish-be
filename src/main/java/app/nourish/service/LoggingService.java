package app.nourish.service;

import app.nourish.domain.LogEntry;
import app.nourish.repo.Store;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Write-heavy logging service (FR-18) plus precomputed-on-read trend rollups
 * (FR-20). Supports idempotent writes via a client-supplied id so the offline
 * queue can retry safely (NFR-2).
 */
@Service
public class LoggingService {

    private final Store<LogEntry> logs;

    public LoggingService(Store<LogEntry> logs) {
        this.logs = logs;
    }

    public LogEntry add(String userId, LogEntry entry) {
        entry.setUserId(userId);
        if (entry.getTimestamp() == null) entry.setTimestamp(Instant.now());

        // Idempotency: if a clientId was already stored, return the existing row.
        if (entry.getClientId() != null && !entry.getClientId().isBlank()) {
            LogEntry existing = logs.findWhere(l ->
                    userId.equals(l.getUserId()) && entry.getClientId().equals(l.getClientId()))
                    .stream().findFirst().orElse(null);
            if (existing != null) return existing;
        }
        return logs.save(entry);
    }

    public List<LogEntry> list(String userId, LogEntry.Type type) {
        return logs.findWhere(l -> userId.equals(l.getUserId()) && (type == null || l.getType() == type))
                .stream()
                .sorted(Comparator.comparing(LogEntry::getTimestamp).reversed())
                .toList();
    }

    public List<LogEntry> recent(String userId, Duration window) {
        Instant cutoff = Instant.now().minus(window);
        return logs.findWhere(l -> userId.equals(l.getUserId())
                        && l.getTimestamp() != null
                        && l.getTimestamp().isAfter(cutoff))
                .stream()
                .sorted(Comparator.comparing(LogEntry::getTimestamp).reversed())
                .toList();
    }

    /** FR-20: trend rollups — weight trend line, adherence streak, daily calories. */
    public Map<String, Object> trends(String userId, int days) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        List<LogEntry> window = logs.findWhere(l -> userId.equals(l.getUserId())
                && l.getTimestamp() != null && l.getTimestamp().isAfter(cutoff));

        // Weight trend line (date -> latest weight that day).
        Map<String, Double> weightByDay = new TreeMap<>();
        window.stream().filter(l -> l.getType() == LogEntry.Type.WEIGHT && l.getWeightKg() != null)
                .sorted(Comparator.comparing(LogEntry::getTimestamp))
                .forEach(l -> weightByDay.put(day(l.getTimestamp()), l.getWeightKg()));

        // Daily calories (date -> sum).
        Map<String, Integer> caloriesByDay = new TreeMap<>();
        window.stream().filter(l -> l.getType() == LogEntry.Type.FOOD && l.getCalories() != null)
                .forEach(l -> caloriesByDay.merge(day(l.getTimestamp()), l.getCalories(), Integer::sum));

        // Adherence streak: consecutive days (ending today) with at least one food log.
        int streak = 0;
        LocalDate d = LocalDate.now(ZoneOffset.UTC);
        List<String> loggedDays = window.stream()
                .filter(l -> l.getType() == LogEntry.Type.FOOD)
                .map(l -> day(l.getTimestamp())).distinct().toList();
        while (loggedDays.contains(d.toString())) {
            streak++;
            d = d.minusDays(1);
        }

        long workoutCount = window.stream().filter(l -> l.getType() == LogEntry.Type.WORKOUT).count();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("weightTrend", toPoints(weightByDay));
        out.put("caloriesByDay", toIntPoints(caloriesByDay));
        out.put("adherenceStreakDays", streak);
        out.put("workoutsInWindow", workoutCount);
        out.put("windowDays", days);
        return out;
    }

    private List<Map<String, Object>> toPoints(Map<String, Double> src) {
        List<Map<String, Object>> pts = new ArrayList<>();
        src.forEach((k, v) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", k);
            p.put("value", v);
            pts.add(p);
        });
        return pts;
    }

    private List<Map<String, Object>> toIntPoints(Map<String, Integer> src) {
        List<Map<String, Object>> pts = new ArrayList<>();
        src.forEach((k, v) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", k);
            p.put("value", v);
            pts.add(p);
        });
        return pts;
    }

    private String day(Instant ts) {
        return ts.atZone(ZoneOffset.UTC).toLocalDate().toString();
    }
}
