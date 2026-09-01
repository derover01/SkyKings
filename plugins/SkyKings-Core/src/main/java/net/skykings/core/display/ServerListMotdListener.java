package net.skykings.core.display;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

/** Zweizeilige Serverlisten-Beschreibung im SkyKings-Look. */
public final class ServerListMotdListener implements Listener {

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        event.setMotd(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS"
                + ChatColor.DARK_GRAY + " • " + ChatColor.WHITE + "OP-SKYPVP "
                + ChatColor.DARK_GRAY + "× " + ChatColor.GRAY + "1.8"
                + "\n"
                + ChatColor.WHITE + "Oldschool Feeling. "
                + ChatColor.AQUA + "Next-Level Systeme.");
    }
}
