package app.nourish.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Thread-safe in-memory {@link Store}. IDs are read/assigned through the supplied
 * accessor functions so the store stays free of reflection.
 */
public class InMemoryStore<T> implements Store<T> {
    private final Map<String, T> data = new ConcurrentHashMap<>();
    private final Function<T, String> idGetter;
    private final BiConsumer<T, String> idSetter;

    public InMemoryStore(Function<T, String> idGetter, BiConsumer<T, String> idSetter) {
        this.idGetter = idGetter;
        this.idSetter = idSetter;
    }

    @Override
    public synchronized T save(T entity) {
        String id = idGetter.apply(entity);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            idSetter.accept(entity, id);
        }
        data.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public List<T> findWhere(Predicate<T> predicate) {
        List<T> out = new ArrayList<>();
        for (T v : data.values()) {
            if (predicate.test(v)) out.add(v);
        }
        return out;
    }

    @Override
    public void deleteById(String id) {
        data.remove(id);
    }

    @Override
    public void deleteWhere(Predicate<T> predicate) {
        data.values().removeIf(predicate);
    }
}
