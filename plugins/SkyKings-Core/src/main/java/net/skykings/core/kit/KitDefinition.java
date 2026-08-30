package net.skykings.core.kit;

import net.skykings.core.model.Rank;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Technische Beschreibung eines Kits (Phase 1B: nur Registry-Infrastruktur, siehe
 * docs/ROADMAP.md Phase 3 fuer die tatsaechliche Kit-Progression).
 *
 * <p>Items werden ueber eine {@link Supplier}-"ItemFactory" statt einer festen Liste erzeugt,
 * damit jede Vergabe frische, unabhaengige {@link ItemStack}-Instanzen bekommt (kein geteilter
 * mutabler Zustand zwischen zwei Spielern/Vergaben desselben Kits).
 */
public final class KitDefinition {

    private final String id;
    private final String displayName;
    private final Rank requiredRank;
    private final long cooldownMillis;
    private final Supplier<List<ItemStack>> itemFactory;
    private final List<PotionEffect> potionEffects;

    private KitDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.requiredRank = builder.requiredRank;
        this.cooldownMillis = builder.cooldownMillis;
        this.itemFactory = builder.itemFactory;
        this.potionEffects = Collections.unmodifiableList(new ArrayList<>(builder.potionEffects));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Rank getRequiredRank() {
        return requiredRank;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    /** Erzeugt eine frische Kopie der Kit-Items. Kann bei jedem Aufruf eine neue Liste liefern. */
    public List<ItemStack> createItems() {
        List<ItemStack> items = itemFactory.get();
        return items == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(items));
    }

    /** Sofort beim Kit-Erhalt anzuwendende Effekte (z. B. ein temporaerer Buff), unveraenderlich. */
    public List<PotionEffect> getPotionEffects() {
        return potionEffects;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String displayName;
        private Rank requiredRank = Rank.SPIELER;
        private long cooldownMillis;
        private Supplier<List<ItemStack>> itemFactory = Collections::emptyList;
        private final List<PotionEffect> potionEffects = new ArrayList<>();

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder requiredRank(Rank requiredRank) {
            this.requiredRank = Objects.requireNonNull(requiredRank, "requiredRank");
            return this;
        }

        public Builder cooldownMillis(long cooldownMillis) {
            if (cooldownMillis < 0) {
                throw new IllegalArgumentException("cooldownMillis darf nicht negativ sein: " + cooldownMillis);
            }
            this.cooldownMillis = cooldownMillis;
            return this;
        }

        public Builder itemFactory(Supplier<List<ItemStack>> itemFactory) {
            this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
            return this;
        }

        public Builder addPotionEffect(PotionEffect effect) {
            this.potionEffects.add(Objects.requireNonNull(effect, "effect"));
            return this;
        }

        public KitDefinition build() {
            return new KitDefinition(this);
        }
    }
}
