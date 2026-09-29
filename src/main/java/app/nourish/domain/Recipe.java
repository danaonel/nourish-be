package app.nourish.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe (BRD §8). Tags describe which constraints it matches so the UI can
 * show <em>why</em> a suggestion fits (journey Stage 2 opportunity).
 */
public class Recipe {
    private String id;
    private String userId; // null = shared library recipe
    private String name;
    private List<String> ingredients = new ArrayList<>();
    private List<String> steps = new ArrayList<>();
    private List<String> tags = new ArrayList<>(); // e.g. protein-source, prep-effort, dairy-free
    private String prepEffort; // LOW, MEDIUM, HIGH
    private Integer estimatedCalories;
    private Integer proteinGrams;
    private boolean freezerFriendly;
    private boolean confirmedWorks; // FR-27 "this works for me"

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> v) { this.ingredients = v; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> v) { this.steps = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public String getPrepEffort() { return prepEffort; }
    public void setPrepEffort(String v) { this.prepEffort = v; }
    public Integer getEstimatedCalories() { return estimatedCalories; }
    public void setEstimatedCalories(Integer v) { this.estimatedCalories = v; }
    public Integer getProteinGrams() { return proteinGrams; }
    public void setProteinGrams(Integer v) { this.proteinGrams = v; }
    public boolean isFreezerFriendly() { return freezerFriendly; }
    public void setFreezerFriendly(boolean v) { this.freezerFriendly = v; }
    public boolean isConfirmedWorks() { return confirmedWorks; }
    public void setConfirmedWorks(boolean v) { this.confirmedWorks = v; }
}
