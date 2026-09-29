package app.nourish.domain;

import java.time.Instant;

/**
 * Unified log entry covering the four core logs (FR-18): FOOD, WEIGHT, DRINK,
 * WORKOUT. A single type keeps quick-add fast and the offline sync queue simple.
 */
public class LogEntry {
    public enum Type { FOOD, WEIGHT, DRINK, WORKOUT }

    private String id;
    private String userId;
    private Type type;
    private Instant timestamp = Instant.now();

    // FOOD / DRINK
    private String label;
    private Integer calories;
    private Integer proteinGrams;

    // WEIGHT
    private Double weightKg;

    // DRINK
    private Integer volumeMl;
    private boolean caffeinated;

    // WORKOUT
    private String activity;
    private Integer durationMin;
    private Integer steps;

    // shared optional metadata
    private String photoUrl;
    private String locationTag;
    private String clientId; // idempotency key from the offline queue

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant v) { this.timestamp = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public Integer getCalories() { return calories; }
    public void setCalories(Integer v) { this.calories = v; }
    public Integer getProteinGrams() { return proteinGrams; }
    public void setProteinGrams(Integer v) { this.proteinGrams = v; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double v) { this.weightKg = v; }
    public Integer getVolumeMl() { return volumeMl; }
    public void setVolumeMl(Integer v) { this.volumeMl = v; }
    public boolean isCaffeinated() { return caffeinated; }
    public void setCaffeinated(boolean v) { this.caffeinated = v; }
    public String getActivity() { return activity; }
    public void setActivity(String v) { this.activity = v; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer v) { this.durationMin = v; }
    public Integer getSteps() { return steps; }
    public void setSteps(Integer v) { this.steps = v; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String v) { this.photoUrl = v; }
    public String getLocationTag() { return locationTag; }
    public void setLocationTag(String v) { this.locationTag = v; }
    public String getClientId() { return clientId; }
    public void setClientId(String v) { this.clientId = v; }
}
