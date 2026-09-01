package net.skykings.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Verhindert 1.8-Client-Ghostitems, wenn direkt nach einem Drop ein Command/GUI geoeffnet wird. */
public final class InventoryDropSyncListener implements Listener {
    private final JavaPlugin plugin;

    public InventoryDropSyncListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.updateInventory();
            }
        });
    }
}
