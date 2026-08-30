package net.skykings.combat.loot;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schuetzt Death-Drops fuer den legitimen Killer.
 *
 * <p>Wichtig: Direkt beim {@link #protectDeathDrops(Location, UUID)}-Aufruf existieren die
 * eigentlichen Death-Drop-Entities noch nicht. Deshalb wird zuerst ein Snapshot aller bereits
 * vorhandenen Item-Entity-UUIDs im Suchradius erstellt. Einen Tick spaeter werden nur NEU
 * hinzugekommene Item-Entities geschuetzt. Dadurch werden herumliegende Alt-Items nicht
 * versehentlich dem Killer zugeordnet.
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
        if (deathLocation == null || killerUuid == null) {
            return;
        }
        World world = deathLocation.getWorld();
        if (world == null) {
            return;
        }

        Set<UUID> preExistingItemIds = new HashSet<>();
        for (Entity entity : world.getNearbyEntities(deathLocation, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
            if (entity instanceof Item) {
                preExistingItemIds.add(entity.getUniqueId());
            }
        }

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTask(plugin, () -> {
            long expiresAt = System.currentTimeMillis() + protectionDurationMillis;
            for (Entity entity : world.getNearbyEntities(deathLocation, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
                if (!(entity instanceof Item)) {
                    continue;
                }
                UUID entityId = entity.getUniqueId();
                if (preExistingItemIds.contains(entityId)) {
                    continue;
                }
                protectedDrops.put(entityId, new ProtectedDrop(killerUuid, expiresAt));
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

        // Der Besitzer darf aufheben, aber der Tracking-Eintrag wird HIER noch nicht entfernt.
        // Ein anderes Plugin kann denselben Pickup spaeter noch canceln. Erst ein erfolgreicher,
        // am MONITOR-Punkt weiterhin nicht gecancelter Pickup entfernt den Schutz (LootPickupListener).
        return drop.ownerUuid.equals(player.getUniqueId());
    }

    @Override
    public void forget(Item item) {
        protectedDrops.remove(item.getUniqueId());
    }
}
