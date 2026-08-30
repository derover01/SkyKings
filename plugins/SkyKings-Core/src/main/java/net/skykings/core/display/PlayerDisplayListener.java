package net.skykings.core.display;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerDisplayListener implements Listener {

    private final PlayerDisplayService displayService;

    public PlayerDisplayListener(PlayerDisplayService displayService) {
        this.displayService = displayService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        displayService.refreshTab(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String prefix = displayService.prefixFor(player);
        event.setFormat(prefix + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "%1$s"
                + ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "%2$s");
    }
}
