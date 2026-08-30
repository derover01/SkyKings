package net.skykings.core.kit;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class KitRegistryImpl implements KitRegistry {

    private final Map<String, KitDefinition> kits = new ConcurrentHashMap<>();

    @Override
    public void register(KitDefinition kit) {
        Objects.requireNonNull(kit, "kit");
        KitDefinition existing = kits.putIfAbsent(kit.getId(), kit);
        if (existing != null) {
            throw new IllegalArgumentException("Kit mit ID '" + kit.getId() + "' ist bereits registriert.");
        }
    }

    @Override
    public Optional<KitDefinition> get(String id) {
        return Optional.ofNullable(kits.get(id));
    }

    @Override
    public boolean contains(String id) {
        return kits.containsKey(id);
    }

    @Override
    public Collection<KitDefinition> getAll() {
        return Collections.unmodifiableCollection(kits.values());
    }

    @Override
    public void unregister(String id) {
        kits.remove(id);
    }
}
