package app.nourish.web;

import app.nourish.domain.GroceryList;
import app.nourish.domain.LogEntry;
import app.nourish.domain.MealPlan;
import app.nourish.domain.NotificationPrefs;
import app.nourish.domain.Profile;
import app.nourish.domain.Recipe;
import app.nourish.repo.Store;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DP-2: full data export and hard delete across every collection.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final Store<Profile> profiles;
    private final Store<Recipe> recipes;
    private final Store<MealPlan> mealPlans;
    private final Store<GroceryList> groceries;
    private final Store<LogEntry> logs;
    private final Store<NotificationPrefs> prefs;
    private final CurrentUser currentUser;

    public AccountController(Store<Profile> profiles, Store<Recipe> recipes, Store<MealPlan> mealPlans,
                             Store<GroceryList> groceries, Store<LogEntry> logs,
                             Store<NotificationPrefs> prefs, CurrentUser currentUser) {
        this.profiles = profiles;
        this.recipes = recipes;
        this.mealPlans = mealPlans;
        this.groceries = groceries;
        this.logs = logs;
        this.prefs = prefs;
        this.currentUser = currentUser;
    }

    @GetMapping("/export")
    public Map<String, Object> export(HttpServletRequest req) {
        String u = currentUser.resolve(req);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("profile", profiles.findWhere(p -> u.equals(p.getUserId())));
        out.put("recipes", recipes.findWhere(r -> u.equals(r.getUserId())));
        out.put("mealPlans", mealPlans.findWhere(m -> u.equals(m.getUserId())));
        out.put("groceryLists", groceries.findWhere(g -> u.equals(g.getUserId())));
        out.put("logs", logs.findWhere(l -> u.equals(l.getUserId())));
        out.put("notificationPrefs", prefs.findWhere(n -> u.equals(n.getUserId())));
        return out;
    }

    @DeleteMapping
    public Map<String, String> delete(HttpServletRequest req) {
        String u = currentUser.resolve(req);
        profiles.deleteWhere(p -> u.equals(p.getUserId()));
        recipes.deleteWhere(r -> u.equals(r.getUserId()));
        mealPlans.deleteWhere(m -> u.equals(m.getUserId()));
        groceries.deleteWhere(g -> u.equals(g.getUserId()));
        logs.deleteWhere(l -> u.equals(l.getUserId()));
        prefs.deleteWhere(n -> u.equals(n.getUserId()));
        return Map.of("status", "deleted");
    }
}
