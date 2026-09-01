package net.skykings.combat.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistente Rueckkehrposition fuer isolierte Combat-Events.
 *
 * Der erste erfolgreiche Teleport nach dem Event-Join speichert automatisch die Position vor
 * dem Event. Kehrt der Spieler normal dorthin zurueck, wird der Eintrag entfernt. Bleibt der
 * Spieler dagegen auf einem Death-Screen/offline oder crasht der Server, ueberlebt der Eintrag
 * in event-returns.yml und wird beim naechsten Respawn/Join verbraucht.
 */
public final class EventReturnRecoveryService implements Listener {
    private static EventReturnRecoveryService instance;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, StoredLocation> pending = new LinkedHashMap<UUID, StoredLocation>();

    private EventReturnRecoveryService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "event-returns.yml");
        load();
    }

    /** Idempotente Lazy-Installation, damit bestehende Event-Bootstraps nicht erweitert werden muessen. */
    public static synchronized void installIfPossible() {
        if (activeInstance() != null) return;
        instance = null; // alte Instanz nach Plugin-Reload niemals als aktiv behandeln
        try {
            Plugin raw = Bukkit.getPluginManager().getPlugin("SkyKings-Combat");
            if (!(raw instanceof JavaPlugin) || !raw.isEnabled()) return;
            JavaPlugin plugin = (JavaPlugin) raw;
            EventReturnRecoveryService service = new EventReturnRecoveryService(plugin);
            Bukkit.getPluginManager().registerEvents(service, plugin);
            instance = service;
            plugin.getLogger().info("Event Return Recovery aktiviert: event-returns.yml");
        } catch (Throwable ignored) {
            // Unit-Tests ohne Bukkit-Server duerfen EventParticipationService weiterhin laden.
        }
    }

    /** Read-only Runtime-Health fuer /skcheck und Diagnose. */
    public static synchronized boolean isInstalled() {
        return activeInstance() != null;
    }

    /** Anzahl persistenter Rueckkehrpositionen, die noch auf Join/Respawn warten. */
    public static synchronized int pendingCount() {
        EventReturnRecoveryService active = activeInstance();
        return active == null ? -1 : active.pendingSize();
    }

    /**
     * Statische Felder ueberleben Bukkit-/Plugin-Reloads. Deshalb gilt eine Instanz nur dann als
     * gesund, wenn sie zur aktuell geladenen und aktivierten Combat-Plugininstanz gehoert.
     */
    private static EventReturnRecoveryService activeInstance() {
        EventReturnRecoveryService current = instance;
        if (current == null) return null;
        try {
            Plugin loaded = Bukkit.getPluginManager().getPlugin("SkyKings-Combat");
            if (loaded != current.plugin || !current.plugin.isEnabled()) return null;
            return current;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private synchronized int pendingSize() {
        return pending.size();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        EventParticipationService.Participation participation = EventParticipationService.global().get(uuid);

        if (participation != null) {
            // Nur der erste Event-Teleport zaehlt. Spaetere Teleports innerhalb der Arena duerfen
            // die echte Rueckkehrposition niemals mit einer Eventposition ueberschreiben.
            if (!pending.containsKey(uuid) && event.getFrom() != null) {
                pending.put(uuid, StoredLocation.from(event.getFrom()));
                save();
            }
            return;
        }

        StoredLocation stored = pending.get(uuid);
        if (stored == null || event.getTo() == null) return;
        Location expected = stored.resolve();
        if (expected != null && sameReturnLocation(expected, event.getTo())) {
            pending.remove(uuid);
            save();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (EventParticipationService.global().isInEvent(uuid)) return;
        StoredLocation stored = pending.remove(uuid);
        if (stored == null) return;
        Location location = stored.resolve();
        if (location == null) {
            pending.put(uuid, stored);
            return;
        }
        event.setRespawnLocation(location);
        save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        if (!pending.containsKey(uuid) || EventParticipationService.global().isInEvent(uuid)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || EventParticipationService.global().isInEvent(uuid)) return;
            if (player.isDead()) return; // Death-Screen bleibt fuer PlayerRespawnEvent reserviert.

            StoredLocation stored = pending.get(uuid);
            if (stored == null) return;
            Location location = stored.resolve();
            if (location == null) return;
            if (player.teleport(location)) {
                pending.remove(uuid);
                save();
            }
        });
    }

    private boolean sameReturnLocation(Location expected, Location actual) {
        if (expected.getWorld() == null || actual.getWorld() == null) return false;
        if (!expected.getWorld().getName().equals(actual.getWorld().getName())) return false;
        return expected.distanceSquared(actual) <= 4.0D;
    }

    private synchronized void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("pending");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String base = "pending." + key + ".";
                String world = yaml.getString(base + "world");
                if (world == null || world.trim().isEmpty()) continue;
                pending.put(uuid, new StoredLocation(world,
                        yaml.getDouble(base + "x"), yaml.getDouble(base + "y"), yaml.getDouble(base + "z"),
                        (float) yaml.getDouble(base + "yaw"), (float) yaml.getDouble(base + "pitch")));
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Ungueltiger Event-Return-Eintrag " + key + ": " + ex.getMessage());
            }
        }
    }

    private synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, StoredLocation> entry : pending.entrySet()) {
            String base = "pending." + entry.getKey() + ".";
            StoredLocation location = entry.getValue();
            yaml.set(base + "world", location.world);
            yaml.set(base + "x", location.x);
            yaml.set(base + "y", location.y);
            yaml.set(base + "z", location.z);
            yaml.set(base + "yaw", location.yaw);
            yaml.set(base + "pitch", location.pitch);
        }

        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Event-Return-Datenordner konnte nicht erstellt werden: " + parent);
                return;
            }
            yaml.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Event-Returns konnten nicht sicher gespeichert werden: " + ex.getMessage());
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
        }
    }

    private static final class StoredLocation {
        final String world;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;

        StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        static StoredLocation from(Location location) {
            return new StoredLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch());
        }

        Location resolve() {
            World loaded = Bukkit.getWorld(world);
            return loaded == null ? null : new Location(loaded, x, y, z, yaw, pitch);
        }
    }
}
