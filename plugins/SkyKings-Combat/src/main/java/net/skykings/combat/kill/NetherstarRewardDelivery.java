package net.skykings.combat.kill;

import org.bukkit.entity.Player;

/** Liefert den berechneten PvP-Netherstern-Reward aus. */
public interface NetherstarRewardDelivery {
    void give(Player player, long amount);
}
