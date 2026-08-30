package net.skykings.core.listener;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Verbindet nur den Bukkit-Lifecycle mit den Services - keine Business-Logik hier (siehe CLAUDE.md Punkt 9). */
public final class PlayerLifecycleListener implements Listener {

    private final PlayerProfileService profileService;
    private final CooldownService cooldownService;
    private final Logger logger;

    public PlayerLifecycleListener(PlayerProfileService profileService, CooldownService cooldownService, Logger logger) {
        this.profileService = profileService;
        this.cooldownService = cooldownService;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        try {
            profileService.loadOrCreate(uuid, name);
            cooldownService.loadForPlayer(uuid);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Konnte PlayerProfile fuer " + name + " (" + uuid + ") nicht laden", e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profileService.updatePresence(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        profileService.saveAndUnload(uuid);
        cooldownService.unloadForPlayer(uuid);
    }
}
