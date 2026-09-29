package app.nourish.ai;

import app.nourish.domain.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic hard-constraint filter (APP-TECHNICAL-PLAN.md §4.1, risk #4).
 *
 * <p>This is the <em>actual</em> safety boundary. Whether a suggestion comes from
 * an LLM or the local planner, it must pass through here before it is ever shown.
 * Allergies, intolerances, and ethical/values-based exclusions (SG-6) are treated
 * with equal rigor — a suggestion that mentions any excluded term is rejected.</p>
 */
@Component
public class ConstraintFilter {

    public record Result(boolean allowed, String violation) {
        public static Result ok() { return new Result(true, null); }
        public static Result blocked(String violation) { return new Result(false, violation); }
    }

    /**
     * Checks a free-text suggestion (recipe name + ingredients joined) against the
     * profile's hard exclusions.
     */
    public Result check(Profile profile, String text) {
        if (text == null) return Result.ok();
        String haystack = text.toLowerCase(Locale.ROOT);

        Result r = scan(haystack, profile.getAllergies(), "allergy");
        if (!r.allowed()) return r;
        r = scan(haystack, profile.getIntolerances(), "intolerance");
        if (!r.allowed()) return r;
        // SG-6: ethical exclusions enforced like allergies.
        r = scan(haystack, profile.getEthicalConstraints(), "ethical constraint");
        if (!r.allowed()) return r;
        return Result.ok();
    }

    /**
     * Whether a menu item should be suppressed rather than shown (FR-16).
     * Suppression list and trigger foods are hidden/de-emphasized, not neutrally listed.
     */
    public boolean shouldSuppress(Profile profile, String text) {
        if (text == null) return false;
        String haystack = text.toLowerCase(Locale.ROOT);
        return containsAny(haystack, profile.getMenuSuppressionList())
                || containsAny(haystack, profile.getTriggerFoods());
    }

    private Result scan(String haystack, List<String> terms, String kind) {
        if (terms == null) return Result.ok();
        for (String term : terms) {
            if (term == null || term.isBlank()) continue;
            if (haystack.contains(term.toLowerCase(Locale.ROOT))) {
                return Result.blocked(kind + ": " + term);
            }
        }
        return Result.ok();
    }

    private boolean containsAny(String haystack, List<String> terms) {
        if (terms == null) return false;
        for (String term : terms) {
            if (term == null || term.isBlank()) continue;
            if (haystack.contains(term.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
