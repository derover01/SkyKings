package net.skykings.combat.loot;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

/** Setzt {@link LootProtectionService} um: blockiert Pickups fremder, noch geschuetzter Death-Drops. */
public final class LootPickupListener implements Listener {

    private final LootProtectionService lootProtectionService;

    public LootPickupListener(LootProtectionService lootProtectionService) {
        this.lootProtectionService = lootProtectionService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        Item item = event.getItem();
        Player player = event.getPlayer();
        if (!lootProtectionService.canPickup(item, player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Cleanup erst ganz am Ende des Event-Pfads: Nur wenn der Pickup bis MONITOR weiterhin
     * nicht gecancelt wurde, gilt er als erfolgreich genug, um den Schutz zu entfernen.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSuccessfulPickup(PlayerPickupItemEvent event) {
        lootProtectionService.forget(event.getItem());
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        lootProtectionService.forget(event.getEntity());
    }
}
