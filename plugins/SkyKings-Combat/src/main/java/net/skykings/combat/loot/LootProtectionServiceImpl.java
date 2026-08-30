package net.skykings.combat.loot;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sucht die tatsaechlichen Death-Drop-Entities einen Tick nach dem Tod in einem kleinen Radius
 * um den Sterbeort (die Items existieren erst, nachdem der Server {@code PlayerDeathEvent}
 * fertig verarbeitet hat) und markiert nur diese als geschuetzt.
 */
public final class LootProtectionServiceImpl implements LootProtectionService {

    private static final double SEARCH_RADIUS = 2.0;

    private static final class ProtectedDrop {
        private final UUID ownerUuid;
        private final long expiresAt;

        ProtectedDrop(UUID ownerUuid, long expiresAt) {
            this.ownerUuid = ownerUuid;
            this.expiresAt = expiresAt;
        }
    }

    private final Plugin plugin;
    private final long protectionDurationMillis;
    private final Map<UUID, ProtectedDrop> protectedDrops = new ConcurrentHashMap<>();

    public LootProtectionServiceImpl(Plugin plugin, long protectionDurationMillis) {
        if (protectionDurationMillis <= 0) {
            throw new IllegalArgumentException("protectionDurationMillis muss positiv sein: " + protectionDurationMillis);
        }
        this.plugin = plugin;
        this.protectionDurationMillis = protectionDurationMillis;
    }

    @Override
    public void protectDeathDrops(Location deathLocation, UUID killerUuid) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTask(plugin, () -> {
            long expiresAt = System.currentTimeMillis() + protectionDurationMillis;
            for (Entity entity : deathLocation.getWorld().getNearbyEntities(deathLocation, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
                if (entity instanceof Item) {
                    protectedDrops.put(entity.getUniqueId(), new ProtectedDrop(killerUuid, expiresAt));
                }
            }
        });
    }

    @Override
    public boolean canPickup(Item item, Player player) {
        UUID entityId = item.getUniqueId();
        ProtectedDrop drop = protectedDrops.get(entityId);
        if (drop == null) {
            return true;
        }
        if (System.currentTimeMillis() >= drop.expiresAt) {
            protectedDrops.remove(entityId, drop);
            return true;
        }
        if (drop.ownerUuid.equals(player.getUniqueId())) {
            protectedDrops.remove(entityId, drop);
            return true;
        }
        return false;
    }

    @Override
    public void forget(Item item) {
        protectedDrops.remove(item.getUniqueId());
    }
}
