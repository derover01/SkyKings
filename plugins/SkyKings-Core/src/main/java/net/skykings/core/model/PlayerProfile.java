package net.skykings.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Reines Datenmodell fuer ein SkyKings-Spielerprofil.
 *
 * <p>Enthaelt bewusst keine Geschaeftslogik (siehe docs/ARCHITECTURE.md, Entwicklungsregel 1):
 * Validierung von Betraegen, Rang-Wechsel-Regeln etc. leben in den jeweiligen Services
 * (RankService, EconomyService). Diese Klasse garantiert nur die Basis-Invariante,
 * dass Coins/Netherstars nie negativ werden.
 */
public final class PlayerProfile {

    private final UUID uuid;
    private volatile String lastKnownName;
    private volatile Rank rank;
    private volatile long coins;
    private volatile long netherstars;
    private final long createdAt;
    private volatile long lastSeen;
    private volatile boolean newbieProtectionDisabled;

    public PlayerProfile(UUID uuid, String lastKnownName, Rank rank, long coins, long netherstars,
                          long createdAt, long lastSeen) {
        this(uuid, lastKnownName, rank, coins, netherstars, createdAt, lastSeen, false);
    }

    public PlayerProfile(UUID uuid, String lastKnownName, Rank rank, long coins, long netherstars,
                          long createdAt, long lastSeen, boolean newbieProtectionDisabled) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.lastKnownName = Objects.requireNonNull(lastKnownName, "lastKnownName");
        this.rank = Objects.requireNonNull(rank, "rank");
        setCoins(coins);
        setNetherstars(netherstars);
        this.createdAt = createdAt;
        this.lastSeen = lastSeen;
        this.newbieProtectionDisabled = newbieProtectionDisabled;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = Objects.requireNonNull(lastKnownName, "lastKnownName");
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = Objects.requireNonNull(rank, "rank");
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        if (coins < 0) {
            throw new IllegalArgumentException("coins darf nicht negativ sein: " + coins);
        }
        this.coins = coins;
    }

    public long getNetherstars() {
        return netherstars;
    }

    public void setNetherstars(long netherstars) {
        if (netherstars < 0) {
            throw new IllegalArgumentException("netherstars darf nicht negativ sein: " + netherstars);
        }
        this.netherstars = netherstars;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Ob die Newbie-Protection (siehe SkyKings-Combat) fuer diesen Spieler vorzeitig/permanent
     * beendet wurde (z. B. weil er selbst zuerst einen anderen Spieler angegriffen hat).
     * Solange {@code false}, gilt weiterhin das 20-Minuten-Zeitfenster ab {@link #getCreatedAt()}.
     */
    public boolean isNewbieProtectionDisabled() {
        return newbieProtectionDisabled;
    }

    public void setNewbieProtectionDisabled(boolean newbieProtectionDisabled) {
        this.newbieProtectionDisabled = newbieProtectionDisabled;
    }

    @Override
    public String toString() {
        return "PlayerProfile{uuid=" + uuid + ", lastKnownName='" + lastKnownName + "', rank=" + rank
                + ", coins=" + coins + ", netherstars=" + netherstars + ", createdAt=" + createdAt
                + ", lastSeen=" + lastSeen + ", newbieProtectionDisabled=" + newbieProtectionDisabled + '}';
    }
}
