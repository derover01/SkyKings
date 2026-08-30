package net.skykings.combat.kill;

import net.skykings.combat.tag.CombatTagService;
import net.skykings.combat.tag.LastAttackerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verbindet echte PvP-Tode UND Combat-Logout mit {@link CombatKillService} - beide nutzen
 * denselben Pfad ({@link PlayerDeathEvent}), siehe Auftrag Phase 2, Abschnitt 6.
 *
 * <p>Combat-Logout: Verlaesst ein Spieler den Server waehrend eines aktiven Combat Tags, wird
 * er ueber {@code setHealth(0)} regulaer getoetet, BEVOR die Verbindung vollstaendig getrennt
 * wird - das loest den normalen {@link PlayerDeathEvent}-Pfad aus (normale Drops, kein
 * Item-Duping, keine doppelte Verarbeitung). Da Bukkits eigenes {@code Player#getKiller()} nur
 * ein kurzes (~5s) Zeitfenster kennt, wird der zuletzt bekannte Angreifer separat ueber
 * {@link LastAttackerService} ermittelt und fuer genau diesen einen Tod "durchgereicht".
 */
public final class CombatDeathListener implements Listener {

    private final CombatKillService combatKillService;
    private final CombatTagService combatTagService;
    private final LastAttackerService lastAttackerService;
    private final Logger logger;
    private final Function<UUID, Player> playerResolver;

    private final Set<UUID> pendingCombatLogout = ConcurrentHashMap.newKeySet();
    private final java.util.Map<UUID, UUID> pendingCombatLogoutKiller = new ConcurrentHashMap<>();

    public CombatDeathListener(CombatKillService combatKillService, CombatTagService combatTagService,
                                LastAttackerService lastAttackerService, Logger logger) {
        this(combatKillService, combatTagService, lastAttackerService, logger, Bukkit::getPlayer);
    }

    /** Sichtbar fuer Tests: erlaubt, die Bukkit-Online-Spieler-Suche durch ein Test-Double zu ersetzen. */
    CombatDeathListener(CombatKillService combatKillService, CombatTagService combatTagService,
                         LastAttackerService lastAttackerService, Logger logger, Function<UUID, Player> playerResolver) {
        this.combatKillService = combatKillService;
        this.combatTagService = combatTagService;
        this.lastAttackerService = lastAttackerService;
        this.logger = logger;
        this.playerResolver = playerResolver;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimUuid = victim.getUniqueId();

        Player killer = resolveKiller(victim, victimUuid);
        combatTagService.clear(victimUuid);
        lastAttackerService.clear(victimUuid);
        if (killer != null) {
            combatTagService.clear(killer.getUniqueId());
        }

        combatKillService.handleDeath(victim, killer);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!combatTagService.isTagged(uuid)) {
            return;
        }
        if (player.isDead()) {
            // Bereits ueber einen anderen Pfad gestorben (z. B. regulaerer PvP-Hit im selben
            // Tick) - keine doppelte Todesverarbeitung ausloesen.
            return;
        }

        UUID attackerUuid = lastAttackerService.getLastAttacker(uuid);
        pendingCombatLogout.add(uuid);
        if (attackerUuid != null) {
            pendingCombatLogoutKiller.put(uuid, attackerUuid);
        }

        try {
            player.setHealth(0.0);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Combat-Logout-Verarbeitung fuer " + uuid + " fehlgeschlagen", e);
            pendingCombatLogout.remove(uuid);
            pendingCombatLogoutKiller.remove(uuid);
        }
    }

    /**
     * Ein Consumable/One-Shot-Lookup: liefert fuer einen Combat-Logout-Tod den zuvor ermittelten
     * Angreifer (ggf. {@code null}, falls keiner mehr feststellbar war - dann wird NIE auf
     * {@code getKiller()} zurueckgefallen, um keinen erfundenen Killer zu erzeugen). Fuer einen
     * normalen Tod wird ganz normal {@code victim.getKiller()} verwendet.
     */
    private Player resolveKiller(Player victim, UUID victimUuid) {
        if (pendingCombatLogout.remove(victimUuid)) {
            UUID attackerUuid = pendingCombatLogoutKiller.remove(victimUuid);
            return attackerUuid != null ? playerResolver.apply(attackerUuid) : null;
        }
        return victim.getKiller();
    }
}
