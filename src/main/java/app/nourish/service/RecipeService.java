package app.nourish.service;

import app.nourish.domain.Profile;
import app.nourish.domain.Recipe;
import app.nourish.repo.Store;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Manages the user's recipe collection and the "what already works" library
 * (FR-5, FR-27).
 */
@Service
public class RecipeService {

    private final Store<Recipe> recipes;
    private final ProfileService profileService;

    public RecipeService(Store<Recipe> recipes, ProfileService profileService) {
        this.recipes = recipes;
        this.profileService = profileService;
    }

    public List<Recipe> forUser(String userId) {
        return recipes.findWhere(r -> userId.equals(r.getUserId()));
    }

    public Recipe save(String userId, Recipe recipe) {
        recipe.setUserId(userId);
        return recipes.save(recipe);
    }

    /** FR-27: confirm/reject a recipe as "this works for me", updating the FR-5 library. */
    public Optional<Recipe> setConfirmed(String userId, String recipeId, boolean confirmed) {
        Optional<Recipe> found = recipes.findById(recipeId)
                .filter(r -> userId.equals(r.getUserId()));
        found.ifPresent(r -> {
            r.setConfirmedWorks(confirmed);
            recipes.save(r);
            Profile p = profileService.getOrCreate(userId);
            if (confirmed && !p.getWorksForMeRecipeIds().contains(recipeId)) {
                p.getWorksForMeRecipeIds().add(recipeId);
            } else if (!confirmed) {
                p.getWorksForMeRecipeIds().remove(recipeId);
            }
            profileService.save(userId, p);
        });
        return found;
    }
}
