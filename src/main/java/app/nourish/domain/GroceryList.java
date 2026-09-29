package app.nourish.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A grocery list generated from an active meal plan (FR-11, BRD §8),
 * grouped by store section and preferred source.
 */
public class GroceryList {
    private String id;
    private String userId;
    private String mealPlanId;
    private List<Section> sections = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMealPlanId() { return mealPlanId; }
    public void setMealPlanId(String v) { this.mealPlanId = v; }
    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> v) { this.sections = v; }

    public static class Section {
        private String name;             // e.g. Produce, Proteins
        private String preferredSource;  // FR-3 respected sourcing
        private List<Item> items = new ArrayList<>();
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getPreferredSource() { return preferredSource; }
        public void setPreferredSource(String v) { this.preferredSource = v; }
        public List<Item> getItems() { return items; }
        public void setItems(List<Item> v) { this.items = v; }
    }

    public static class Item {
        private String label;
        private boolean checked;
        public String getLabel() { return label; }
        public void setLabel(String v) { this.label = v; }
        public boolean isChecked() { return checked; }
        public void setChecked(boolean v) { this.checked = v; }
    }
}
