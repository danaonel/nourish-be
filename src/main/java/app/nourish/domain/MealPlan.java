package app.nourish.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A rolling weekly meal plan (BRD §4.2, §8). Each meal carries a human-readable
 * {@code whyItFits} rationale so trust builds quickly (journey Stage 2).
 */
public class MealPlan {
    private String id;
    private String userId;
    private String weekStart; // ISO date of week start
    private String source;    // AI_GENERATED, USER_LIBRARY, CLINICIAN_IMPORTED
    private List<Day> days = new ArrayList<>();
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String v) { this.weekStart = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
    public List<Day> getDays() { return days; }
    public void setDays(List<Day> v) { this.days = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }

    public static class Day {
        private String dayOfWeek;
        private boolean plannedException; // FR-8: built-in expected event, not a slip
        private String plannedExceptionLabel;
        private List<Meal> meals = new ArrayList<>();
        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String v) { this.dayOfWeek = v; }
        public boolean isPlannedException() { return plannedException; }
        public void setPlannedException(boolean v) { this.plannedException = v; }
        public String getPlannedExceptionLabel() { return plannedExceptionLabel; }
        public void setPlannedExceptionLabel(String v) { this.plannedExceptionLabel = v; }
        public List<Meal> getMeals() { return meals; }
        public void setMeals(List<Meal> v) { this.meals = v; }
    }

    public static class Meal {
        private String slot; // BREAKFAST, LUNCH, DINNER, SNACK
        private String recipeId;
        private String name;
        private Integer estimatedCalories;
        private Integer proteinGrams;
        private String whyItFits; // e.g. "matches your protein+salad pattern"
        private boolean fromWorksForMeLibrary;
        public String getSlot() { return slot; }
        public void setSlot(String v) { this.slot = v; }
        public String getRecipeId() { return recipeId; }
        public void setRecipeId(String v) { this.recipeId = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public Integer getEstimatedCalories() { return estimatedCalories; }
        public void setEstimatedCalories(Integer v) { this.estimatedCalories = v; }
        public Integer getProteinGrams() { return proteinGrams; }
        public void setProteinGrams(Integer v) { this.proteinGrams = v; }
        public String getWhyItFits() { return whyItFits; }
        public void setWhyItFits(String v) { this.whyItFits = v; }
        public boolean isFromWorksForMeLibrary() { return fromWorksForMeLibrary; }
        public void setFromWorksForMeLibrary(boolean v) { this.fromWorksForMeLibrary = v; }
    }
}
