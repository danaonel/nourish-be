package app.nourish.repo;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Minimal storage abstraction. The default implementation is in-memory so the
 * service runs with no external dependency; a MongoDB-backed implementation can
 * be dropped in behind the same interface under the {@code mongo} profile
 * (APP-TECHNICAL-PLAN.md §3.2).
 */
public interface Store<T> {
    T save(T entity);
    Optional<T> findById(String id);
    List<T> findAll();
    List<T> findWhere(Predicate<T> predicate);
    void deleteById(String id);
    void deleteWhere(Predicate<T> predicate);
}
