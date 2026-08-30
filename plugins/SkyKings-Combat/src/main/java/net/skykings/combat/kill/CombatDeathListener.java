package net.skykings.combat.kill;

import net.skykings.combat.cosmetic.KillCosmeticService;
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

/** Verbindet echte PvP-Tode und Combat-Logout mit dem zentralen Kill-Pfad. */
public final class CombatDeathListener implements Listener {

    private final CombatKillService combatKillService;
    private final CombatTagService combatTagService;
    private final LastAttackerService lastAttackerService;
    private final KillMessageService killMessageService;
    private final KillCosmeticService killCosmeticService;
    private final Logger logger;
    private final Function<UUID, Player> playerResolver;

    private final Set<UUID> pendingCombatLogout = ConcurrentHashMap.newKeySet();
    private final java.util.Map<UUID, UUID> pendingCombatLogoutKiller = new ConcurrentHashMap<>();

    public CombatDeathListener(CombatKillService combatKillService, CombatTagService combatTagService,
                                LastAttackerService lastAttackerService, Logger logger) {
        this(combatKillService, combatTagService, lastAttackerService, null, null, logger, Bukkit::getPlayer);
    }

    public CombatDeathListener(CombatKillService combatKillService, CombatTagService combatTagService,
                                LastAttackerService lastAttackerService, KillMessageService killMessageService,
                                KillCosmeticService killCosmeticService, Logger logger) {
        this(combatKillService, combatTagService, lastAttackerService, killMessageService, killCosmeticService,
                logger, Bukkit::getPlayer);
    }

    CombatDeathListener(CombatKillService combatKillService, CombatTagService combatTagService,
                         LastAttackerService lastAttackerService, Logger logger, Function<UUID, Player> playerResolver) {
        this(combatKillService, combatTagService, lastAttackerService, null, null, logger, playerResolver);
    }

    CombatDeathListener(CombatKillService combatKillService, CombatTagService combatTagService,
                         LastAttackerService lastAttackerService, KillMessageService killMessageService,
                         KillCosmeticService killCosmeticService, Logger logger, Function<UUID, Player> playerResolver) {
        this.combatKillService = combatKillService;
        this.combatTagService = combatTagService;
        this.lastAttackerService = lastAttackerService;
        this.killMessageService = killMessageService;
        this.killCosmeticService = killCosmeticService;
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

        if (killer != null && !killer.getUniqueId().equals(victimUuid)) {
            if (killMessageService != null) event.setDeathMessage(killMessageService.create(killer, victim));
            if (killCosmeticService != null) killCosmeticService.play(killer, victim.getLocation());
        }

        combatKillService.handleDeath(victim, killer);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!combatTagService.isTagged(uuid) || player.isDead()) return;

        UUID attackerUuid = lastAttackerService.getLastAttacker(uuid);
        pendingCombatLogout.add(uuid);
        if (attackerUuid != null) pendingCombatLogoutKiller.put(uuid, attackerUuid);

        try {
            player.setHealth(0.0);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Combat-Logout-Verarbeitung für " + uuid + " fehlgeschlagen", e);
            pendingCombatLogout.remove(uuid);
            pendingCombatLogoutKiller.remove(uuid);
        }
    }

    private Player resolveKiller(Player victim, UUID victimUuid) {
        if (pendingCombatLogout.remove(victimUuid)) {
            UUID attackerUuid = pendingCombatLogoutKiller.remove(victimUuid);
            return attackerUuid != null ? playerResolver.apply(attackerUuid) : null;
        }
        return victim.getKiller();
    }
}
