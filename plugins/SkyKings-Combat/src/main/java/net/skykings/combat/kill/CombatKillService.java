package net.skykings.combat.kill;

import org.bukkit.entity.Player;

/**
 * Zentrale Kill-/Death-Verarbeitung (siehe Auftrag Phase 2, Abschnitt 9).
 *
 * <p>Andere Todesursachen als ein legitimer Player-Kill duerfen KEINE PvP-Rewards ausloesen -
 * dafuer {@code killer == null} uebergeben. Der Killstreak des Opfers wird in jedem Fall
 * zurueckgesetzt (unabhaengig von der Todesursache).
 */
public interface CombatKillService {

    void handleDeath(Player victim, Player killer);
}
