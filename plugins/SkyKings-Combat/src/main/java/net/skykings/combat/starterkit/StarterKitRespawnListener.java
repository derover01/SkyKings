package net.skykings.combat.starterkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Wendet das Death-Starter-Kit beim Respawn an (siehe Auftrag Phase 2, Abschnitt 4). */
public final class StarterKitRespawnListener implements Listener {

    private final DeathStarterKitService starterKitService;

    public StarterKitRespawnListener(DeathStarterKitService starterKitService) {
        this.starterKitService = starterKitService;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        starterKitService.apply(player);
    }
}
