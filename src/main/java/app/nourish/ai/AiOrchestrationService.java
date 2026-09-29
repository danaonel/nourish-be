package app.nourish.ai;

import app.nourish.domain.GoalType;
import app.nourish.domain.MealPlan;
import app.nourish.domain.Profile;
import app.nourish.domain.Recipe;
import app.nourish.repo.Store;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Single entry point for AI-driven generation (APP-TECHNICAL-PLAN.md §3, §4.1).
 *
 * <p>All persona behavior differences and safety guardrails are enforced here in
 * code, not left to prompt wording. When no LLM key is configured the service
 * uses a deterministic local planner so the app is fully functional; the
 * generation path is identical in that every candidate meal is validated by the
 * {@link ConstraintFilter} before inclusion.</p>
 */
@Service
public class AiOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestrationService.class);

    private final ConstraintFilter constraintFilter;
    private final Store<Recipe> recipeStore;
    private final AnthropicClient anthropic;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiOrchestrationService(ConstraintFilter constraintFilter,
                                  Store<Recipe> recipeStore,
                                  AnthropicClient anthropic) {
        this.constraintFilter = constraintFilter;
        this.recipeStore = recipeStore;
        this.anthropic = anthropic;
    }

    public boolean isLlmEnabled() {
        return anthropic.isConfigured();
    }

    /**
     * Generates a weekly plan. Candidates (from the LLM or local planner) are each
     * run through the hard-constraint filter; anything that violates an exclusion
     * is dropped and replaced from the safe pool.
     */
    public MealPlan generateWeeklyPlan(Profile profile) {
        List<Recipe> candidates = safeCandidates(profile);

        MealPlan plan = new MealPlan();
        plan.setUserId(profile.getUserId());
        plan.setWeekStart(startOfWeek().toString());

        // Ask Claude to curate/order the candidate pool per slot. This only
        // REORDERS a pool that has already passed the constraint filter — the LLM
        // cannot introduce a food the user excluded. On any failure we fall back
        // to a deterministic rotation over the same pool.
        Optional<Map<String, List<Recipe>>> curated = isLlmEnabled()
                ? curateWithClaude(profile, candidates)
                : Optional.empty();
        plan.setSource(curated.isPresent() ? "AI_GENERATED" : "LOCAL_PLANNER");

        Map<String, Integer> slotCursor = new LinkedHashMap<>();
        int rotation = 0;
        for (DayOfWeek dow : DayOfWeek.values()) {
            MealPlan.Day day = new MealPlan.Day();
            day.setDayOfWeek(dow.name());

            // FR-8: honor recurring planned exceptions rather than flag them.
            Profile.PlannedException matched = matchException(profile, dow);
            if (matched != null) {
                day.setPlannedException(true);
                day.setPlannedExceptionLabel(matched.getLabel());
            }

            for (String slot : List.of("BREAKFAST", "LUNCH", "DINNER", "SNACK")) {
                if (candidates.isEmpty()) break;
                Recipe r;
                if (curated.isPresent() && !curated.get().getOrDefault(slot, List.of()).isEmpty()) {
                    List<Recipe> pool = curated.get().get(slot);
                    int idx = slotCursor.merge(slot, 1, Integer::sum) - 1;
                    r = pool.get(idx % pool.size());
                } else {
                    r = candidates.get(rotation % candidates.size());
                    rotation++;
                }
                day.getMeals().add(toMeal(profile, slot, r));
            }
            plan.getDays().add(day);
        }
        return plan;
    }

    /**
     * Uses Claude to organize the (already constraint-safe) candidate recipes into
     * per-slot preferences. The model is given the persona system prompt and the
     * candidate names, and returns JSON mapping each slot to an ordered list of
     * names. We map names back to real vetted recipes and re-run the constraint
     * filter as the hard boundary — anything unrecognized or violating is dropped.
     */
    private Optional<Map<String, List<Recipe>>> curateWithClaude(Profile profile, List<Recipe> candidates) {
        if (candidates.isEmpty()) return Optional.empty();

        Map<String, Recipe> byName = new LinkedHashMap<>();
        for (Recipe r : candidates) byName.put(r.getName().toLowerCase(Locale.ROOT), r);

        StringBuilder user = new StringBuilder();
        user.append("Organize a weekly meal plan for this user using ONLY the candidate meals below.\n");
        user.append("Do not invent new meals. Return STRICT JSON with this exact shape and nothing else:\n");
        user.append("{\"BREAKFAST\":[names],\"LUNCH\":[names],\"DINNER\":[names],\"SNACK\":[names]}\n");
        user.append("Each list is candidate meal names ordered best-first for that slot.\n\n");
        user.append("User goal: ").append(profile.getGoalType()).append('\n');
        if (profile.getCookingTolerance() != null) {
            user.append("Cooking tolerance: ").append(profile.getCookingTolerance()).append('\n');
        }
        if (!profile.getAppetiteCurbingFoods().isEmpty()) {
            user.append("Foods that curb their appetite: ")
                    .append(String.join(", ", profile.getAppetiteCurbingFoods())).append('\n');
        }
        user.append("\nCandidate meals:\n");
        for (Recipe r : candidates) {
            user.append("- ").append(r.getName())
                    .append(" (").append(r.getEstimatedCalories() == null ? "?" : r.getEstimatedCalories())
                    .append(" kcal, ").append(r.getProteinGrams() == null ? "?" : r.getProteinGrams())
                    .append("g protein)\n");
        }

        Optional<String> resp = anthropic.complete(
                PersonaPrompts.systemPromptFor(profile.getGoalType()), user.toString(), 1024);
        if (resp.isEmpty()) return Optional.empty();

        try {
            String json = extractJson(resp.get());
            JsonNode root = mapper.readTree(json);
            Map<String, List<Recipe>> out = new LinkedHashMap<>();
            for (String slot : List.of("BREAKFAST", "LUNCH", "DINNER", "SNACK")) {
                JsonNode arr = root.path(slot);
                if (!arr.isArray()) continue;
                List<Recipe> ordered = new ArrayList<>();
                for (JsonNode n : arr) {
                    Recipe r = byName.get(n.asText("").toLowerCase(Locale.ROOT));
                    // Hard boundary: only accept known recipes that still pass the filter.
                    if (r != null && passes(profile, r) && !ordered.contains(r)) {
                        ordered.add(r);
                    }
                }
                if (!ordered.isEmpty()) out.put(slot, ordered);
            }
            return out.isEmpty() ? Optional.empty() : Optional.of(out);
        } catch (Exception e) {
            log.warn("Could not parse Claude meal plan JSON, using local planner: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Pulls the first JSON object out of a response that may include prose fences. */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return text;
    }

    /**
     * Builds the safe candidate pool: user "works for me" recipes first (FR-5),
     * then goal-appropriate library recipes, all passing the constraint filter.
     */
    private List<Recipe> safeCandidates(Profile profile) {
        List<Recipe> all = new ArrayList<>();

        // FR-5: user-confirmed recipes first.
        for (Recipe r : recipeStore.findWhere(x ->
                profile.getUserId().equals(x.getUserId()) && x.isConfirmedWorks())) {
            if (passes(profile, r)) all.add(r);
        }

        for (Recipe r : SeedRecipes.forGoal(profile.getGoalType())) {
            if (passes(profile, r) && all.stream().noneMatch(a -> a.getName().equalsIgnoreCase(r.getName()))) {
                all.add(r);
            }
        }
        return all;
    }

    private boolean passes(Profile profile, Recipe r) {
        String text = r.getName() + " " + String.join(" ", r.getIngredients());
        return constraintFilter.check(profile, text).allowed();
    }

    private MealPlan.Meal toMeal(Profile profile, String slot, Recipe r) {
        MealPlan.Meal m = new MealPlan.Meal();
        m.setSlot(slot);
        m.setRecipeId(r.getId());
        m.setName(r.getName());
        m.setEstimatedCalories(r.getEstimatedCalories());
        m.setProteinGrams(r.getProteinGrams());
        m.setFromWorksForMeLibrary(r.isConfirmedWorks()
                && profile.getUserId().equals(r.getUserId()));
        m.setWhyItFits(explain(profile, r));
        return m;
    }

    /** Human-readable rationale so trust builds quickly (journey Stage 2). */
    private String explain(Profile profile, Recipe r) {
        if (r.isConfirmedWorks() && profile.getUserId().equals(r.getUserId())) {
            return "From your \"what works\" library";
        }
        List<String> reasons = new ArrayList<>();
        if (profile.getGoalType() == GoalType.LOSE && r.getProteinGrams() != null && r.getProteinGrams() >= 25) {
            reasons.add("high-protein for appetite control");
        }
        if (profile.getGoalType() == GoalType.GAIN && r.getEstimatedCalories() != null && r.getEstimatedCalories() >= 600) {
            reasons.add("calorie-dense for your surplus");
        }
        if ("LOW".equalsIgnoreCase(r.getPrepEffort())
                && profile.getCookingTolerance() != null
                && profile.getCookingTolerance().toUpperCase(Locale.ROOT).contains("HATE")) {
            reasons.add("low-prep, matches your routine");
        }
        if (r.isFreezerFriendly()) {
            reasons.add("prep-ahead / freezer friendly");
        }
        for (String src : profile.getPreferredSources()) {
            if (r.getTags().stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(src.toLowerCase(Locale.ROOT)))) {
                reasons.add("sourced per your preference (" + src + ")");
                break;
            }
        }
        return reasons.isEmpty() ? "Fits your goal and constraints" : String.join("; ", reasons);
    }

    private Profile.PlannedException matchException(Profile profile, DayOfWeek dow) {
        for (Profile.PlannedException pe : profile.getPlannedExceptions()) {
            if (pe.getDays() != null && pe.getDays().stream().anyMatch(d -> d.equalsIgnoreCase(dow.name()))) {
                return pe;
            }
        }
        return null;
    }

    private LocalDate startOfWeek() {
        LocalDate today = LocalDate.now();
        return today.minusDays(today.getDayOfWeek().getValue() - 1L);
    }
}
