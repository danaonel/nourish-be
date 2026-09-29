package app.nourish.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The persona-defining profile (BRD §4.1, §8). Captures biometrics, goal, and the
 * medical / ethical / behavioral constraints that filter every suggestion.
 */
public class Profile {
    private String id;
    private String userId;

    // FR-1 biometrics + goal
    private Integer age;
    private String sex;
    private Double heightCm;
    private Double currentWeightKg;
    private String activityLevel;
    private GoalType goalType = GoalType.MAINTAIN;
    private Double targetWeightKg;

    // FR-2 medical
    private List<String> medicalConditions = new ArrayList<>();
    private List<Medication> medications = new ArrayList<>();
    private List<String> allergies = new ArrayList<>();
    private List<String> intolerances = new ArrayList<>();

    // FR-3 ethical / values-based constraints (first-class filters, like allergies)
    private List<String> ethicalConstraints = new ArrayList<>();
    private List<String> preferredSources = new ArrayList<>();

    // FR-4 behavioral preferences
    private List<String> appetiteCurbingFoods = new ArrayList<>();
    private List<String> triggerFoods = new ArrayList<>();
    private List<String> menuSuppressionList = new ArrayList<>();

    // FR-5 "what already works" library (recipe ids the user confirmed)
    private List<String> worksForMeRecipeIds = new ArrayList<>();

    // FR-6 routine / logistics
    private List<String> shopDays = new ArrayList<>();
    private List<String> prepDays = new ArrayList<>();
    private String cookingTolerance; // e.g. HATES_COOKING, GRAB_AND_GO, ENJOYS

    // FR-7 caffeine / sleep
    private String caffeineCutoff;
    private List<String> sleepAids = new ArrayList<>();

    // FR-8 recurring planned exceptions
    private List<PlannedException> plannedExceptions = new ArrayList<>();

    // FR-22 workout equipment + existing content library
    private List<String> equipment = new ArrayList<>();
    private List<String> workoutPlaylists = new ArrayList<>();

    // SG-2 / FR-21 recovery guardrail toggles
    private boolean calorieLoggingEnabled = true;
    private boolean weightLoggingEnabled = true;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public Double getCurrentWeightKg() { return currentWeightKg; }
    public void setCurrentWeightKg(Double currentWeightKg) { this.currentWeightKg = currentWeightKg; }
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }
    public Double getTargetWeightKg() { return targetWeightKg; }
    public void setTargetWeightKg(Double targetWeightKg) { this.targetWeightKg = targetWeightKg; }
    public List<String> getMedicalConditions() { return medicalConditions; }
    public void setMedicalConditions(List<String> v) { this.medicalConditions = v; }
    public List<Medication> getMedications() { return medications; }
    public void setMedications(List<Medication> v) { this.medications = v; }
    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> v) { this.allergies = v; }
    public List<String> getIntolerances() { return intolerances; }
    public void setIntolerances(List<String> v) { this.intolerances = v; }
    public List<String> getEthicalConstraints() { return ethicalConstraints; }
    public void setEthicalConstraints(List<String> v) { this.ethicalConstraints = v; }
    public List<String> getPreferredSources() { return preferredSources; }
    public void setPreferredSources(List<String> v) { this.preferredSources = v; }
    public List<String> getAppetiteCurbingFoods() { return appetiteCurbingFoods; }
    public void setAppetiteCurbingFoods(List<String> v) { this.appetiteCurbingFoods = v; }
    public List<String> getTriggerFoods() { return triggerFoods; }
    public void setTriggerFoods(List<String> v) { this.triggerFoods = v; }
    public List<String> getMenuSuppressionList() { return menuSuppressionList; }
    public void setMenuSuppressionList(List<String> v) { this.menuSuppressionList = v; }
    public List<String> getWorksForMeRecipeIds() { return worksForMeRecipeIds; }
    public void setWorksForMeRecipeIds(List<String> v) { this.worksForMeRecipeIds = v; }
    public List<String> getShopDays() { return shopDays; }
    public void setShopDays(List<String> v) { this.shopDays = v; }
    public List<String> getPrepDays() { return prepDays; }
    public void setPrepDays(List<String> v) { this.prepDays = v; }
    public String getCookingTolerance() { return cookingTolerance; }
    public void setCookingTolerance(String v) { this.cookingTolerance = v; }
    public String getCaffeineCutoff() { return caffeineCutoff; }
    public void setCaffeineCutoff(String v) { this.caffeineCutoff = v; }
    public List<String> getSleepAids() { return sleepAids; }
    public void setSleepAids(List<String> v) { this.sleepAids = v; }
    public List<PlannedException> getPlannedExceptions() { return plannedExceptions; }
    public void setPlannedExceptions(List<PlannedException> v) { this.plannedExceptions = v; }
    public List<String> getEquipment() { return equipment; }
    public void setEquipment(List<String> v) { this.equipment = v; }
    public List<String> getWorkoutPlaylists() { return workoutPlaylists; }
    public void setWorkoutPlaylists(List<String> v) { this.workoutPlaylists = v; }
    public boolean isCalorieLoggingEnabled() { return calorieLoggingEnabled; }
    public void setCalorieLoggingEnabled(boolean v) { this.calorieLoggingEnabled = v; }
    public boolean isWeightLoggingEnabled() { return weightLoggingEnabled; }
    public void setWeightLoggingEnabled(boolean v) { this.weightLoggingEnabled = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }

    public static class Medication {
        private String name;
        private String timingNote; // e.g. "empty stomach", "avoid with dairy"
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTimingNote() { return timingNote; }
        public void setTimingNote(String timingNote) { this.timingNote = timingNote; }
    }

    public static class PlannedException {
        private String label;       // e.g. "Weekend brunch with partner"
        private List<String> days = new ArrayList<>(); // e.g. [SATURDAY, SUNDAY]
        private String note;
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public List<String> getDays() { return days; }
        public void setDays(List<String> days) { this.days = days; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
