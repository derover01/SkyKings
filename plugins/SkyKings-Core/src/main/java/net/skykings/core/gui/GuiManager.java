package net.skykings.core.gui;

import net.skykings.core.sound.SoundFeedback;
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

/** Zentrales, Anti-Dupe-sicheres GUI-Framework fuer alle SkyKings-Menues. */
public final class GuiManager implements Listener {

    private static volatile GuiManager active;
    private final Map<UUID, GuiSession> openSessions = new ConcurrentHashMap<UUID, GuiSession>();

    public GuiManager() {
        active = this;
    }

    /** Aktive Core-GUI-Instanz. Core erzeugt diese vor allen Feature-Services. */
    public static GuiManager active() {
        if (active == null) throw new IllegalStateException("GuiManager ist noch nicht initialisiert");
        return active;
    }

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
        if (!(clicker instanceof Player)) return;
        GuiSession session = openSessions.get(clicker.getUniqueId());
        if (session == null) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != null && session.getInventory().equals(event.getClickedInventory())) {
            SoundFeedback.click((Player) clicker);
            session.handleClick((Player) clicker, event, event.getRawSlot());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        HumanEntity closer = event.getPlayer();
        if (!(closer instanceof Player)) return;
        UUID uuid = closer.getUniqueId();
        GuiSession session = openSessions.get(uuid);
        if (session != null && session.getInventory().equals(event.getInventory())) {
            openSessions.remove(uuid);
            session.handleClose();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openSessions.remove(event.getPlayer().getUniqueId());
    }
}
