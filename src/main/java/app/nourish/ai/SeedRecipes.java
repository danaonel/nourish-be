package app.nourish.ai;

import app.nourish.domain.GoalType;
import app.nourish.domain.Recipe;

import java.util.List;

/**
 * A small built-in recipe library used by the deterministic local planner. In
 * production this would be replaced/augmented by the LLM + nutrition-database
 * grounding (APP-TECHNICAL-PLAN.md §4.2). Kept dairy-free and clearly tagged so
 * the constraint filter and rationale engine have real data to work with.
 */
public final class SeedRecipes {
    private SeedRecipes() {}

    public static List<Recipe> forGoal(GoalType goal) {
        return switch (goal) {
            case GAIN -> highCalorie();
            case LOSE -> highProteinLower();
            default -> balanced();
        };
    }

    private static Recipe r(String name, int cals, int protein, String prep, boolean freezer,
                            List<String> ingredients, List<String> tags) {
        Recipe x = new Recipe();
        x.setName(name);
        x.setEstimatedCalories(cals);
        x.setProteinGrams(protein);
        x.setPrepEffort(prep);
        x.setFreezerFriendly(freezer);
        x.setIngredients(ingredients);
        x.setTags(tags);
        return x;
    }

    private static List<Recipe> highProteinLower() {
        return List.of(
            r("Grilled chicken + big salad", 420, 40, "LOW", true,
                List.of("grilled chicken", "mixed greens", "olive oil", "lemon", "cucumber"),
                List.of("protein-source", "dairy-free", "high-protein")),
            r("Turkey lettuce wraps", 380, 34, "LOW", true,
                List.of("ground turkey", "lettuce", "peppers", "onion", "tamari"),
                List.of("protein-source", "dairy-free", "grab-and-go")),
            r("Salmon + roasted veg", 480, 36, "MEDIUM", false,
                List.of("salmon", "broccoli", "olive oil", "garlic"),
                List.of("protein-source", "dairy-free", "omega-3")),
            r("Egg + veg scramble", 320, 24, "LOW", false,
                List.of("eggs", "spinach", "tomato", "olive oil"),
                List.of("protein-source", "dairy-free", "breakfast")),
            r("Protein + berry bowl", 260, 28, "LOW", false,
                List.of("dairy-free protein", "berries", "chia"),
                List.of("snack", "dairy-free", "high-protein"))
        );
    }

    private static List<Recipe> highCalorie() {
        return List.of(
            r("Beef + rice power bowl", 720, 45, "MEDIUM", true,
                List.of("beef", "rice", "avocado", "black beans", "olive oil"),
                List.of("protein-source", "dairy-free", "calorie-dense")),
            r("Nut butter oat jars", 620, 22, "LOW", true,
                List.of("oats", "peanut butter", "banana", "seeds"),
                List.of("breakfast", "dairy-free", "calorie-dense")),
            r("Chicken thighs + potatoes", 680, 42, "MEDIUM", true,
                List.of("chicken thighs", "potatoes", "olive oil"),
                List.of("protein-source", "dairy-free", "calorie-dense")),
            r("Trail mix + fruit", 480, 14, "LOW", false,
                List.of("nuts", "dried fruit", "seeds"),
                List.of("snack", "dairy-free", "calorie-dense"))
        );
    }

    private static List<Recipe> balanced() {
        return List.of(
            r("Chicken grain bowl", 520, 35, "LOW", true,
                List.of("chicken", "quinoa", "roasted veg", "olive oil"),
                List.of("protein-source", "dairy-free", "balanced")),
            r("Lentil + veg soup", 380, 18, "LOW", true,
                List.of("lentils", "carrot", "celery", "tomato"),
                List.of("plant-based", "dairy-free", "freezer")),
            r("Tofu stir-fry", 440, 24, "MEDIUM", false,
                List.of("tofu", "mixed veg", "tamari", "rice"),
                List.of("plant-based", "dairy-free", "balanced")),
            r("Fruit + seed snack", 220, 6, "LOW", false,
                List.of("apple", "seeds", "nut butter"),
                List.of("snack", "dairy-free"))
        );
    }
}
