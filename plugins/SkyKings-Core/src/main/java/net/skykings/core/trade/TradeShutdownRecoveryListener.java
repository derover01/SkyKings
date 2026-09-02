package net.skykings.core.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gibt noch im RAM-Escrow liegende Trade-Items bei einem sauberen Core-/Server-Stop zurueck.
 * Dadurch geht ein offener Trade beim normalen Shutdown nicht verloren.
 */
final class TradeShutdownRecoveryListener implements Listener {
    private final TradeService tradeService;
    private final JavaPlugin ownerPlugin;
    private boolean recovered;

    TradeShutdownRecoveryListener(TradeService tradeService, JavaPlugin ownerPlugin) {
        this.tradeService = tradeService;
        this.ownerPlugin = ownerPlugin;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (recovered || event.getPlugin() != ownerPlugin) return;
        recovered = true;

        Collection<TradeSession> sessions = tradeService.activeSessionsSnapshot();
        if (sessions.isEmpty()) return;

        Set<UUID> notified = new HashSet<UUID>();
        for (TradeSession session : sessions) {
            if (session == null || session.isFinished()) continue;
            tradeService.finish(session);
            returnOffer(session.getLeft(), notified);
            returnOffer(session.getRight(), notified);
        }

        ownerPlugin.getLogger().warning("Trade-Shutdown-Recovery: " + sessions.size()
                + " offene Session(s) beendet und Escrow-Items zurueckgegeben.");
    }

    private void returnOffer(TradeOffer offer, Set<UUID> notified) {
        if (offer == null) return;
        Player player = Bukkit.getPlayer(offer.getPlayer());
        if (player == null) {
            ownerPlugin.getLogger().severe("Trade-Shutdown-Recovery konnte Items nicht zustellen: Spieler offline "
                    + offer.getPlayer() + ", Items=" + offer.getItems().size());
            return;
        }

        for (ItemStack raw : offer.getItems()) {
            if (raw == null) continue;
            ItemStack item = raw.clone();
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        player.updateInventory();
        if (notified.add(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Ein offener Trade wurde wegen Server-Shutdown abgebrochen. "
                    + ChatColor.GRAY + "Deine angebotenen Items wurden zurueckgegeben.");
        }
    }
}
