package app.nourish.repo;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * MongoDB-backed {@link Store}, one instance per entity type / collection
 * (APP-TECHNICAL-PLAN.md §3.2). Preserves the exact {@link Store} contract so no
 * service code changes when switching from the in-memory store.
 *
 * <p>{@code findById} / {@code save} / {@code deleteById} map directly to Mongo
 * operations by document id. {@code findWhere} / {@code deleteWhere} take a Java
 * {@link Predicate}, which cannot be translated to a native Mongo query
 * generically, so they load the collection and filter in memory. Every service
 * query is scoped by {@code userId}, so collections stay per-user-bounded in
 * practice; if a collection grows large, add a native-query method rather than
 * widening this interface.</p>
 */
public class MongoStore<T> implements Store<T> {

    private final MongoTemplate mongo;
    private final Class<T> type;
    private final String collection;
    private final Function<T, String> idGetter;
    private final BiConsumer<T, String> idSetter;

    public MongoStore(MongoTemplate mongo, Class<T> type, String collection,
                      Function<T, String> idGetter, BiConsumer<T, String> idSetter) {
        this.mongo = mongo;
        this.type = type;
        this.collection = collection;
        this.idGetter = idGetter;
        this.idSetter = idSetter;
    }

    @Override
    public T save(T entity) {
        if (idGetter.apply(entity) == null || idGetter.apply(entity).isBlank()) {
            idSetter.accept(entity, UUID.randomUUID().toString());
        }
        return mongo.save(entity, collection);
    }

    @Override
    public Optional<T> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(mongo.findById(id, type, collection));
    }

    @Override
    public List<T> findAll() {
        return mongo.findAll(type, collection);
    }

    @Override
    public List<T> findWhere(Predicate<T> predicate) {
        return mongo.findAll(type, collection).stream().filter(predicate).toList();
    }

    @Override
    public void deleteById(String id) {
        if (id == null) return;
        mongo.remove(new Query(Criteria.where("_id").is(id)), type, collection);
    }

    @Override
    public void deleteWhere(Predicate<T> predicate) {
        for (T entity : findWhere(predicate)) {
            String id = idGetter.apply(entity);
            if (id != null) deleteById(id);
        }
    }
}
