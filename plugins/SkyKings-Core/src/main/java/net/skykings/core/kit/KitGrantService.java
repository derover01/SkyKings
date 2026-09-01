package net.skykings.core.kit;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;

/** Vergibt registrierte Kits unter Beachtung von Rang und persistentem Cooldown. */
public interface KitGrantService {

    KitGrantResult grant(Player player, String kitId);

    Collection<KitDefinition> getAccessibleKits(Player player);

    /**
     * Vollstaendiger Kit-Katalog fuer Progression-UIs. Als Default leer, damit Test-Doubles und
     * alternative Implementierungen nicht wegen einer reinen Darstellungsfunktion brechen.
     */
    default Collection<KitDefinition> getAllKits() {
        return Collections.emptyList();
    }
}
