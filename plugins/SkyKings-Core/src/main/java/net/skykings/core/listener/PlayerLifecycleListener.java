package net.skykings.core.listener;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.integration.PermissionBridge;
import net.skykings.core.model.PlayerProfile;
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

    public static final String LOAD_FAILURE_MESSAGE =
            "Deine Spielerdaten konnten nicht geladen werden. Bitte versuche es erneut.";

    private final PlayerProfileService profileService;
    private final CooldownService cooldownService;
    private final PermissionBridge permissionBridge;
    private final Logger logger;

    public PlayerLifecycleListener(PlayerProfileService profileService, CooldownService cooldownService,
                                    PermissionBridge permissionBridge, Logger logger) {
        this.profileService = profileService;
        this.cooldownService = cooldownService;
        this.permissionBridge = permissionBridge;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            // Bereits von anderer Quelle abgelehnt (z. B. Ban/Whitelist) - nicht ueberschreiben und
            // keine unnoetige DB-Last erzeugen.
            return;
        }

        UUID uuid = event.getUniqueId();
        String name = event.getName();
        PlayerProfile profile;
        try {
            profile = profileService.loadOrCreate(uuid, name);
            cooldownService.loadForPlayer(uuid);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Konnte PlayerProfile/Cooldowns fuer " + name + " (" + uuid
                    + ") nicht laden - Login wird abgelehnt.", e);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, LOAD_FAILURE_MESSAGE);
            return;
        }

        // Start-Synchronisation SkyKings -> LuckPerms (siehe PermissionBridge-Javadoc: einseitig,
        // niemals umgekehrt). Ein Fehler hier darf den Login NICHT verhindern: die interne
        // SkyKings-Datenbank bleibt die Source of Truth fuer den Rang, LuckPerms ist nur der
        // Permission-/Prefix-/Group-Layer.
        try {
            permissionBridge.syncRank(uuid, profile.getRank());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Konnte LuckPerms-Rang-Synchronisation fuer " + name + " (" + uuid
                    + ") nicht anstossen - Login wird trotzdem fortgesetzt.", e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (profileService.getCached(uuid) == null) {
            // Sollte durch onAsyncPreLogin bereits verhindert worden sein (disallow terminiert die
            // Verbindung vor PlayerJoinEvent). Diese Absicherung faengt nur den Fall ab, dass eine
            // andere Quelle das PreLogin-Ergebnis nachtraeglich veraendert hat - niemals eine
            // ungefangene IllegalStateException aus updatePresence() auf dem Main-Thread zulassen.
            logger.severe("Kein geladenes PlayerProfile fuer " + player.getName() + " (" + uuid
                    + ") beim Join vorhanden - Spieler wird sicherheitshalber gekickt.");
            player.kickPlayer(LOAD_FAILURE_MESSAGE);
            return;
        }
        profileService.updatePresence(uuid, player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        profileService.saveAndUnload(uuid);
        cooldownService.unloadForPlayer(uuid);
    }
}
