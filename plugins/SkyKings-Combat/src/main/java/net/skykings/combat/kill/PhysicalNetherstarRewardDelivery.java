package net.skykings.combat.kill;

import net.skykings.core.item.SkyKingsCurrencyItems;
import org.bukkit.entity.Player;

/** Gibt PvP-Sterne als gebrandete physische SkyKings-Waehrung aus. */
public final class PhysicalNetherstarRewardDelivery implements NetherstarRewardDelivery {
    @Override
    public void give(Player player, long amount) {
        if (player == null || amount <= 0L) return;
        SkyKingsCurrencyItems.give(player, amount);
    }
}
