package net.skykings.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Behebt den bekannten 1.8-Desync beim Q/Ctrl+Q-Droppen aus einem geoeffneten Inventar.
 * Wir veraendern oder canceln den Drop nicht, sondern synchronisieren erst nachdem Bukkit
 * die Slot-Aenderung und das Drop-Entity vollstaendig verarbeitet hat.
 */
public final class InventoryDropSyncListener implements Listener {
    private final JavaPlugin plugin;

    public InventoryDropSyncListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrop(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ClickType click = event.getClick();
        InventoryAction action = event.getAction();
        if (click != ClickType.DROP && click != ClickType.CONTROL_DROP
                && action != InventoryAction.DROP_ONE_SLOT && action != InventoryAction.DROP_ALL_SLOT
                && action != InventoryAction.DROP_ONE_CURSOR && action != InventoryAction.DROP_ALL_CURSOR) return;
        syncLater((Player) event.getWhoClicked());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldDrop(PlayerDropItemEvent event) {
        syncLater(event.getPlayer());
    }

    @SuppressWarnings("deprecation")
    private void syncLater(final Player player) {
        // Zwei Ticks statt sofort/naechster Tick: 1.8 schreibt den Inventarslot teils erst
        // nach dem PlayerDropItemEvent final. Ein zu fruehes updateInventory erzeugt Ghost-Dupes.
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.updateInventory();
            }
        }, 2L);
    }
}
