package net.skykings.core.display;

import net.skykings.core.integration.PaidRankHologramBridge;
import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/** Verbindet Paid-Raenge mit einem optional registrierten Hologramm-Provider. */
public final class PaidRankHologramListener implements Listener {

    private final JavaPlugin plugin;
    private final RankService rankService;

    public PaidRankHologramListener(JavaPlugin plugin, RankService rankService) {
        this.plugin = plugin;
        this.rankService = rankService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PaidRankHologramBridge bridge = resolve();
        if (bridge != null) {
            bridge.remove(event.getPlayer());
        }
    }

    public void refresh(Player player) {
        PaidRankHologramBridge bridge = resolve();
        if (bridge == null) {
            return;
        }
        Rank rank = rankService.getRank(player.getUniqueId());
        if (rank.isPaid()) {
            bridge.refresh(player, rank);
        } else {
            bridge.remove(player);
        }
    }

    private PaidRankHologramBridge resolve() {
        RegisteredServiceProvider<PaidRankHologramBridge> registration =
                plugin.getServer().getServicesManager().getRegistration(PaidRankHologramBridge.class);
        return registration == null ? null : registration.getProvider();
    }
}
