package net.skykings.core.display;

import net.skykings.core.integration.PermissionBridge;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Vergibt fuer explizit konfigurierte Owner Vollzugriff und die separate LuckPerms-Owner-Gruppe. */
public final class OwnerAccessListener implements Listener {

    private final RankDisplayConfig displayConfig;
    private final PermissionBridge permissionBridge;

    public OwnerAccessListener(RankDisplayConfig displayConfig, PermissionBridge permissionBridge) {
        this.displayConfig = displayConfig;
        this.permissionBridge = permissionBridge;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!displayConfig.isConfiguredOwner(player.getName())) {
            return;
        }

        // OP stellt auf dem Legacy-Spigot-Server den unmittelbaren Vollzugriff sicher;
        // LuckPerms erhaelt zusaetzlich Owner-Gruppe + * fuer den normalen Permission-Layer.
        if (!player.isOp()) {
            player.setOp(true);
        }
        permissionBridge.grantOwner(player.getUniqueId());
    }
}
