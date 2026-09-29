package app.nourish.service;

import app.nourish.ai.ConstraintFilter;
import app.nourish.domain.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Real-world contextual guidance (FR-14, FR-15, FR-16). For v1 the venue is
 * supplied by the user ("I'm at X") per the roadmap; menu items are filtered and
 * trigger/suppressed items are de-emphasized rather than neutrally listed.
 *
 * <p>Per DP-3, raw coordinates are not persisted here — only the resolved venue
 * name is used transiently for the recommendation.</p>
 */
@Service
public class ContextService {

    private final ConstraintFilter constraintFilter;

    public ContextService(ConstraintFilter constraintFilter) {
        this.constraintFilter = constraintFilter;
    }

    public record MenuItem(String name, Integer estimatedCalories, Integer proteinGrams) {}
    public record Recommendation(String name, Integer estimatedCalories, Integer proteinGrams, String note) {}
    public record MenuGuidance(String venue, List<Recommendation> recommended, int suppressedCount) {}

    /**
     * Filters a menu against the profile. Excluded items (allergy/ethical) are
     * removed; trigger/suppression-list items are counted but not surfaced (FR-16).
     */
    public MenuGuidance guide(Profile profile, String venue, List<MenuItem> menu) {
        List<Recommendation> recs = new ArrayList<>();
        int suppressed = 0;

        for (MenuItem item : menu) {
            if (!constraintFilter.check(profile, item.name()).allowed()) {
                continue; // hard exclusion — never shown
            }
            if (constraintFilter.shouldSuppress(profile, item.name())) {
                suppressed++;
                continue; // FR-16 — de-emphasize / hide trigger items
            }
            recs.add(new Recommendation(item.name(), item.estimatedCalories(), item.proteinGrams(),
                    rationale(profile, item)));
        }

        // Prefer higher protein for LOSE, higher calories for GAIN.
        recs.sort((a, b) -> {
            switch (profile.getGoalType()) {
                case GAIN -> { return safe(b.estimatedCalories()) - safe(a.estimatedCalories()); }
                default -> { return safe(b.proteinGrams()) - safe(a.proteinGrams()); }
            }
        });

        return new MenuGuidance(venue, recs.stream().limit(5).toList(), suppressed);
    }

    private String rationale(Profile profile, MenuItem item) {
        return switch (profile.getGoalType()) {
            case LOSE -> "Higher protein, fits your appetite-management pattern";
            case GAIN -> "Calorie-dense, supports your surplus";
            case RECOVERY -> "A balanced, steady option";
            default -> "A solid, goal-aligned choice";
        };
    }

    private int safe(Integer v) { return v == null ? 0 : v; }
}
