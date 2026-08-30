package net.skykings.core.kit;

import org.bukkit.entity.Player;

import java.util.Collection;

/** Vergibt registrierte Kits unter Beachtung von Rang und persistentem Cooldown. */
public interface KitGrantService {

    KitGrantResult grant(Player player, String kitId);

    Collection<KitDefinition> getAccessibleKits(Player player);
}
