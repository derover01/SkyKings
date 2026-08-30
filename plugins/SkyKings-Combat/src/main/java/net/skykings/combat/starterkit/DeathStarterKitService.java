package net.skykings.combat.starterkit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Wendet das {@link DeathStarterKit} auf einen respawnenden Spieler an. Kein Rang-Abgleich. */
public final class DeathStarterKitService {

    private final DeathStarterKit kit;
    private final boolean enabled;

    public DeathStarterKitService(DeathStarterKit kit, boolean enabled) {
        this.kit = kit;
        this.enabled = enabled;
    }

    public void apply(Player player) {
        if (!enabled) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        // Defensiv leeren, bevor das Starter-Kit angewendet wird (siehe Auftrag: "Inventar
        // sinnvoll behandeln") - bei aktivem keepInventory/anderen Plugins koennten sonst
        // Alt-Items mit dem Kit vermischt werden.
        inventory.clear();
        inventory.setArmorContents(null);

        inventory.setHelmet(kit.getHelmet());
        inventory.setChestplate(kit.getChestplate());
        inventory.setLeggings(kit.getLeggings());
        inventory.setBoots(kit.getBoots());

        for (ItemStack item : kit.getOtherItems()) {
            inventory.addItem(item);
        }
    }
}
