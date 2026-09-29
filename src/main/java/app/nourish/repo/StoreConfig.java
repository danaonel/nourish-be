package app.nourish.repo;

import app.nourish.domain.GroceryList;
import app.nourish.domain.LogEntry;
import app.nourish.domain.MealPlan;
import app.nourish.domain.NotificationPrefs;
import app.nourish.domain.Profile;
import app.nourish.domain.Recipe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Wires one {@link Store} bean per collection (BRD §8).
 *
 * <ul>
 *   <li>Default (no {@code mongo} profile): {@link InMemoryStore} — zero
 *       infrastructure, data lost on restart.</li>
 *   <li>{@code mongo} profile active: {@link MongoStore} — each user's data is
 *       persisted in MongoDB.</li>
 * </ul>
 */
public class StoreConfig {

    /** In-memory stores — used unless the {@code mongo} profile is active. */
    @Configuration
    @org.springframework.context.annotation.Profile("!mongo")
    static class InMemoryStores {
        @Bean
        public Store<Profile> profileStore() {
            return new InMemoryStore<>(Profile::getId, Profile::setId);
        }

        @Bean
        public Store<Recipe> recipeStore() {
            return new InMemoryStore<>(Recipe::getId, Recipe::setId);
        }

        @Bean
        public Store<MealPlan> mealPlanStore() {
            return new InMemoryStore<>(MealPlan::getId, MealPlan::setId);
        }

        @Bean
        public Store<GroceryList> groceryListStore() {
            return new InMemoryStore<>(GroceryList::getId, GroceryList::setId);
        }

        @Bean
        public Store<LogEntry> logStore() {
            return new InMemoryStore<>(LogEntry::getId, LogEntry::setId);
        }

        @Bean
        public Store<NotificationPrefs> notificationPrefsStore() {
            return new InMemoryStore<>(NotificationPrefs::getId, NotificationPrefs::setId);
        }
    }

    /** MongoDB-backed stores — active only under the {@code mongo} profile. */
    @Configuration
    @org.springframework.context.annotation.Profile("mongo")
    static class MongoStores {
        @Bean
        public Store<Profile> profileStore(MongoTemplate mongo) {
            return new MongoStore<>(mongo, Profile.class, "profiles", Profile::getId, Profile::setId);
        }

        @Bean
        public Store<Recipe> recipeStore(MongoTemplate mongo) {
            return new MongoStore<>(mongo, Recipe.class, "recipes", Recipe::getId, Recipe::setId);
        }

        @Bean
        public Store<MealPlan> mealPlanStore(MongoTemplate mongo) {
            return new MongoStore<>(mongo, MealPlan.class, "mealPlans", MealPlan::getId, MealPlan::setId);
        }

        @Bean
        public Store<GroceryList> groceryListStore(MongoTemplate mongo) {
            return new MongoStore<>(mongo, GroceryList.class, "groceryLists", GroceryList::getId, GroceryList::setId);
        }

        @Bean
        public Store<LogEntry> logStore(MongoTemplate mongo) {
            return new MongoStore<>(mongo, LogEntry.class, "logs", LogEntry::getId, LogEntry::setId);
        }

        @Bean
        public Store<NotificationPrefs> notificationPrefsStore(MongoTemplate mongo) {
            return new MongoStore<>(mongo, NotificationPrefs.class, "notificationPrefs",
                    NotificationPrefs::getId, NotificationPrefs::setId);
        }

        /**
         * Creates indexes on {@code userId} (and {@code (userId, timestamp)} for
         * logs) once at startup, so per-user reads stay cheap as data grows.
         */
        @Bean
        public org.springframework.beans.factory.InitializingBean mongoIndexes(MongoTemplate mongo) {
            return () -> {
                for (String c : java.util.List.of("profiles", "recipes", "mealPlans",
                        "groceryLists", "notificationPrefs")) {
                    mongo.indexOps(c).ensureIndex(
                            new org.springframework.data.mongodb.core.index.Index().on(
                                    "userId", org.springframework.data.domain.Sort.Direction.ASC));
                }
                mongo.indexOps("logs").ensureIndex(
                        new org.springframework.data.mongodb.core.index.Index()
                                .on("userId", org.springframework.data.domain.Sort.Direction.ASC)
                                .on("timestamp", org.springframework.data.domain.Sort.Direction.DESC));
            };
        }
    }
}
