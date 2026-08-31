package net.skykings.combat.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/** Zentrales Event fuer Season-Medaillen, History, Discord und spaetere Quests. */
public final class KingAltarCaptureEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerUuid;

    public KingAltarCaptureEvent(UUID playerUuid) { this.playerUuid = playerUuid; }
    public UUID getPlayerUuid() { return playerUuid; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
