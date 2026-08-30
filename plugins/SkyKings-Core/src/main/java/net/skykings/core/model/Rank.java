package net.skykings.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * SkyKings-Rangmodell (siehe README.md "Ranghierarchie" / docs/GAMEPLAY.md).
 *
 * <p>Die Deklarationsreihenfolge ist die verbindliche Hierarchie: jeder Paid-Rank steht
 * ueber jedem Free-Rank, und innerhalb einer Kategorie gilt die hier festgelegte Reihenfolge.
 * {@link #ordinal()} wird deshalb bewusst als Rangstufe verwendet.
 */
public enum Rank {

    SPIELER(RankCategory.FREE),
    IRON(RankCategory.FREE),
    GOLD(RankCategory.FREE),
    EPIC(RankCategory.FREE),
    DIAMOND(RankCategory.FREE),

    KNIGHT(RankCategory.PAID),
    PHOENIX(RankCategory.PAID),
    ETERNAL(RankCategory.PAID),
    EXILE(RankCategory.PAID),
    ENDLING(RankCategory.PAID),
    KING(RankCategory.PAID);

    /** Free- oder Paid-Rang. Enthaelt bewusst keine Kit-/Preis-Informationen (spaetere Phase). */
    public enum RankCategory {
        FREE,
        PAID
    }

    private final RankCategory category;

    Rank(RankCategory category) {
        this.category = category;
    }

    public RankCategory getCategory() {
        return category;
    }

    public boolean isFree() {
        return category == RankCategory.FREE;
    }

    public boolean isPaid() {
        return category == RankCategory.PAID;
    }

    /** Rangstufe: hoehere Zahl = hoeherer Rang, ueber Free und Paid hinweg vergleichbar. */
    public int getTier() {
        return ordinal();
    }

    public boolean isAtLeast(Rank other) {
        return this.ordinal() >= other.ordinal();
    }

    /** Alle Raenge, die dieser Rang einschliesst (er selbst und alle niedrigeren), aufsteigend sortiert. */
    public List<Rank> getIncludedRanks() {
        List<Rank> included = new ArrayList<>();
        for (Rank rank : values()) {
            if (rank.ordinal() <= this.ordinal()) {
                included.add(rank);
            }
        }
        return included;
    }
}
