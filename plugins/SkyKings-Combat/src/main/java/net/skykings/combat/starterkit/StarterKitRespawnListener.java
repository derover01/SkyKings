package net.skykings.combat.starterkit;

import net.skykings.combat.event.EventParticipationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Wendet das Death-Starter-Kit nur bei normalen Open-World-Respawns an. */
public final class StarterKitRespawnListener implements Listener {

    private final DeathStarterKitService starterKitService;

    public StarterKitRespawnListener(DeathStarterKitService starterKitService) {
        this.starterKitService = starterKitService;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (EventParticipationService.global().isInEvent(player.getUniqueId())) return;
        starterKitService.apply(player);
    }
}
