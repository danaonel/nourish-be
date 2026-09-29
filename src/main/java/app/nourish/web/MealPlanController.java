package app.nourish.web;

import app.nourish.domain.GroceryList;
import app.nourish.domain.MealPlan;
import app.nourish.domain.Profile;
import app.nourish.service.MealPlanService;
import app.nourish.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MealPlanController {

    private final MealPlanService mealPlans;
    private final ProfileService profiles;
    private final CurrentUser currentUser;

    public MealPlanController(MealPlanService mealPlans, ProfileService profiles, CurrentUser currentUser) {
        this.mealPlans = mealPlans;
        this.profiles = profiles;
        this.currentUser = currentUser;
    }

    /** FR-9: generate (or regenerate) the weekly plan. */
    @PostMapping("/meal-plan/generate")
    public MealPlan generate(HttpServletRequest req) {
        Profile p = profiles.getOrCreate(currentUser.resolve(req));
        return mealPlans.generate(p);
    }

    @GetMapping("/meal-plan")
    public ResponseEntity<MealPlan> current(HttpServletRequest req) {
        return mealPlans.current(currentUser.resolve(req))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** FR-11: grocery list from the active plan. */
    @PostMapping("/grocery-list/generate")
    public ResponseEntity<GroceryList> generateGrocery(HttpServletRequest req) {
        String userId = currentUser.resolve(req);
        Profile p = profiles.getOrCreate(userId);
        return mealPlans.current(userId)
                .map(plan -> ResponseEntity.ok(mealPlans.generateGroceryList(p, plan)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/grocery-list")
    public ResponseEntity<GroceryList> currentGrocery(HttpServletRequest req) {
        return mealPlans.currentGroceryList(currentUser.resolve(req))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/grocery-list")
    public GroceryList saveGrocery(HttpServletRequest req, @RequestBody GroceryList list) {
        list.setUserId(currentUser.resolve(req));
        return mealPlans.saveGroceryList(list);
    }
}
