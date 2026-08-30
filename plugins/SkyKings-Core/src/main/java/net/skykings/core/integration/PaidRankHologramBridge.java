package net.skykings.core.integration;

import net.skykings.core.model.Rank;
import org.bukkit.entity.Player;

/**
 * Integrationspunkt fuer Paid-Rank-Hologramme. Die finale Map-/Hologramm-Komponente kann
 * diesen Service implementieren, ohne dass Core eigene ArmorStand-Hitboxen im PvP erzeugt.
 */
public interface PaidRankHologramBridge {

    void refresh(Player player, Rank rank);

    void remove(Player player);
}
