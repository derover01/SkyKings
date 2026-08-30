package net.skykings.core.gui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Technisches GUI-Framework (Phase 1B): verwaltet, welche {@link GuiSession} pro Spieler aktuell
 * geoeffnet ist, cancelt saemtliche Inventar-Interaktionen waehrend eine Session offen ist (kein
 * versehentliches Verschieben/Duplizieren von Items) und raeumt Sessions beim Schliessen sowie
 * beim Verlassen des Servers auf, um Memory-Leaks zu vermeiden.
 *
 * <p>Enthaelt bewusst KEINE konkrete GUI (kein /kit, kein /raenge) - nur das generische Framework.
 */
public final class GuiManager implements Listener {

    private final Map<UUID, GuiSession> openSessions = new ConcurrentHashMap<>();

    public void open(GuiSession session) {
        openSessions.put(session.getPlayer().getUniqueId(), session);
        session.open();
    }

    public GuiSession getOpenSession(UUID uuid) {
        return openSessions.get(uuid);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }
        GuiSession session = openSessions.get(clicker.getUniqueId());
        if (session == null) {
            return;
        }

        // Waehrend eine Session offen ist, wird JEDE Interaktion (oben wie unten) gecancelt, um
        // Item-Verschiebung/-Duplikation ueber die GUI zu verhindern. Ein registrierter Handler
        // kann danach trotzdem gezielt reagieren (z. B. selbst Items vergeben).
        event.setCancelled(true);

        if (event.getClickedInventory() != null && session.getInventory().equals(event.getClickedInventory())) {
            session.handleClick((Player) clicker, event, event.getRawSlot());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        HumanEntity closer = event.getPlayer();
        if (!(closer instanceof Player)) {
            return;
        }
        UUID uuid = closer.getUniqueId();
        GuiSession session = openSessions.get(uuid);
        if (session != null && session.getInventory().equals(event.getInventory())) {
            openSessions.remove(uuid);
            session.handleClose();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Absicherung gegen Memory-Leaks, falls InventoryCloseEvent aus irgendeinem Grund nicht feuert.
        openSessions.remove(event.getPlayer().getUniqueId());
    }
}
