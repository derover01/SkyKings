package net.skykings.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Modulneutrales Event nach einem tatsaechlich erfolgreichen Crate-Open. */
public final class CrateOpenedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String crateType;

    public CrateOpenedEvent(Player player, String crateType) {
        this.player = player;
        this.crateType = crateType == null ? "unknown" : crateType;
    }

    public Player getPlayer() { return player; }
    public String getCrateType() { return crateType; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
