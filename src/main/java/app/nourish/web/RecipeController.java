package app.nourish.web;

import app.nourish.domain.Recipe;
import app.nourish.service.RecipeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipes;
    private final CurrentUser currentUser;

    public RecipeController(RecipeService recipes, CurrentUser currentUser) {
        this.recipes = recipes;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Recipe> list(HttpServletRequest req) {
        return recipes.forUser(currentUser.resolve(req));
    }

    @PostMapping
    public Recipe create(HttpServletRequest req, @RequestBody Recipe recipe) {
        return recipes.save(currentUser.resolve(req), recipe);
    }

    /** FR-27: confirm/reject "this works for me". */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Recipe> confirm(HttpServletRequest req, @PathVariable String id,
                                          @RequestBody Map<String, Boolean> body) {
        boolean confirmed = body.getOrDefault("confirmed", true);
        return recipes.setConfirmed(currentUser.resolve(req), id, confirmed)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
