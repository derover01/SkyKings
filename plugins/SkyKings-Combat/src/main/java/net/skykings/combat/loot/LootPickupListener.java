package net.skykings.combat.loot;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

/** Setzt {@link LootProtectionService} um: blockiert Pickups fremder, noch geschuetzter Death-Drops. */
public final class LootPickupListener implements Listener {

    private final LootProtectionService lootProtectionService;

    public LootPickupListener(LootProtectionService lootProtectionService) {
        this.lootProtectionService = lootProtectionService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        Item item = event.getItem();
        Player player = event.getPlayer();
        if (!lootProtectionService.canPickup(item, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        // Memory-Leak-Schutz: Tracking-Eintrag entfernen, sobald die Entity aus der Welt verschwindet.
        lootProtectionService.forget(event.getEntity());
    }
}
