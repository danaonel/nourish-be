package app.nourish.service;

import app.nourish.ai.AiOrchestrationService;
import app.nourish.domain.GroceryList;
import app.nourish.domain.MealPlan;
import app.nourish.domain.Profile;
import app.nourish.repo.Store;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates and stores weekly plans (FR-9) and derives grocery lists (FR-11)
 * grouped by section and preferred source.
 */
@Service
public class MealPlanService {

    private final AiOrchestrationService ai;
    private final Store<MealPlan> plans;
    private final Store<GroceryList> groceries;

    public MealPlanService(AiOrchestrationService ai, Store<MealPlan> plans, Store<GroceryList> groceries) {
        this.ai = ai;
        this.plans = plans;
        this.groceries = groceries;
    }

    public MealPlan generate(Profile profile) {
        MealPlan plan = ai.generateWeeklyPlan(profile);
        return plans.save(plan);
    }

    public Optional<MealPlan> current(String userId) {
        return plans.findWhere(p -> userId.equals(p.getUserId())).stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
    }

    /** FR-11: grocery list from the active plan, grouped, respecting sourcing (FR-3). */
    public GroceryList generateGroceryList(Profile profile, MealPlan plan) {
        // Collect unique meal names as a simple, deterministic ingredient proxy.
        Map<String, List<String>> bySection = new LinkedHashMap<>();
        for (MealPlan.Day day : plan.getDays()) {
            if (day.isPlannedException()) continue; // planned exceptions aren't shopped for
            for (MealPlan.Meal meal : day.getMeals()) {
                String section = classify(meal.getName());
                bySection.computeIfAbsent(section, k -> new ArrayList<>());
                if (!bySection.get(section).contains(meal.getName())) {
                    bySection.get(section).add(meal.getName());
                }
            }
        }

        GroceryList list = new GroceryList();
        list.setUserId(profile.getUserId());
        list.setMealPlanId(plan.getId());

        String preferred = profile.getPreferredSources().isEmpty()
                ? null : profile.getPreferredSources().get(0);

        bySection.forEach((sectionName, labels) -> {
            GroceryList.Section section = new GroceryList.Section();
            section.setName(sectionName);
            if ("Proteins".equals(sectionName)) {
                section.setPreferredSource(preferred);
            }
            for (String label : labels) {
                GroceryList.Item item = new GroceryList.Item();
                item.setLabel(label);
                section.getItems().add(item);
            }
            list.getSections().add(section);
        });

        return groceries.save(list);
    }

    public Optional<GroceryList> currentGroceryList(String userId) {
        return groceries.findWhere(g -> userId.equals(g.getUserId())).stream().findFirst();
    }

    public GroceryList saveGroceryList(GroceryList list) {
        return groceries.save(list);
    }

    private String classify(String mealName) {
        String n = mealName.toLowerCase();
        if (n.contains("salad") || n.contains("veg") || n.contains("fruit") || n.contains("berry")) return "Produce";
        if (n.contains("chicken") || n.contains("beef") || n.contains("salmon") || n.contains("turkey")
                || n.contains("egg") || n.contains("tofu") || n.contains("protein")) return "Proteins";
        if (n.contains("rice") || n.contains("oat") || n.contains("potato") || n.contains("grain") || n.contains("quinoa")) return "Grains";
        return "Other";
    }
}
