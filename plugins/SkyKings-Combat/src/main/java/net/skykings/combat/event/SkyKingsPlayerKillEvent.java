package net.skykings.combat.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Wird nach einem verarbeiteten, legitimen PvP-Kill gefeuert (siehe Auftrag Phase 2,
 * Abschnitt 17). Andere SkyKings-Module koennen darauf reagieren (z. B. spaeter fuer
 * Death-Messages, Statistiken, Season-Punkte). Rein informativ - der Kill wurde bereits
 * vollstaendig verarbeitet (Reward bereits vergeben), dieses Event kann die Verarbeitung
 * nicht mehr beeinflussen.
 */
public final class SkyKingsPlayerKillEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID killerUuid;
    private final UUID victimUuid;
    private final long baseReward;
    private final double antiFarmMultiplier;
    private final long streakBonus;
    private final long finalReward;
    private final int newKillstreak;

    public SkyKingsPlayerKillEvent(UUID killerUuid, UUID victimUuid, long baseReward, double antiFarmMultiplier,
                                    long streakBonus, long finalReward, int newKillstreak) {
        this.killerUuid = killerUuid;
        this.victimUuid = victimUuid;
        this.baseReward = baseReward;
        this.antiFarmMultiplier = antiFarmMultiplier;
        this.streakBonus = streakBonus;
        this.finalReward = finalReward;
        this.newKillstreak = newKillstreak;
    }

    public UUID getKillerUuid() {
        return killerUuid;
    }

    public UUID getVictimUuid() {
        return victimUuid;
    }

    public long getBaseReward() {
        return baseReward;
    }

    public double getAntiFarmMultiplier() {
        return antiFarmMultiplier;
    }

    public long getStreakBonus() {
        return streakBonus;
    }

    public long getFinalReward() {
        return finalReward;
    }

    public int getNewKillstreak() {
        return newKillstreak;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
