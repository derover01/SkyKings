package net.skykings.combat.spawn;

import net.skykings.combat.tag.CombatTagService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Globaler SkyKings-Spawn mit 3-Sekunden-Teleport ausserhalb von Combat. */
public final class SpawnService implements Listener, CommandExecutor {

    private static final long TELEPORT_DELAY_TICKS = 60L;

    private static final class PendingTeleport {
        final Location origin;
        final BukkitTask task;

        PendingTeleport(Location origin, BukkitTask task) {
            this.origin = origin;
            this.task = task;
        }
    }

    private final JavaPlugin plugin;
    private final CombatTagService combatTagService;
    private final File file;
    private final Map<UUID, PendingTeleport> pending = new HashMap<UUID, PendingTeleport>();
    private volatile Location spawn;

    public SpawnService(JavaPlugin plugin, CombatTagService combatTagService) {
        this.plugin = plugin;
        this.combatTagService = combatTagService;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
        load();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("setspawn")) return handleSetSpawn(player);
        return handleSpawn(player);
    }

    private boolean handleSpawn(final Player player) {
        UUID uuid = player.getUniqueId();
        if (combatTagService.isTagged(uuid)) {
            long seconds = Math.max(1L, (combatTagService.getRemainingMillis(uuid) + 999L) / 1000L);
            player.sendMessage(ChatColor.RED + "Du bist im Kampf. /spawn ist noch " + seconds + "s gesperrt.");
            return true;
        }
        if (pending.containsKey(uuid)) {
            player.sendMessage(ChatColor.YELLOW + "Dein Spawn-Teleport laeuft bereits.");
            return true;
        }
        final Location target = getSpawn();
        if (target == null || target.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "Der SkyKings-Spawn ist noch nicht gesetzt.");
            return true;
        }
        final Location origin = player.getLocation().clone();
        player.sendMessage(ChatColor.GOLD + "Teleport zum Spawn in " + ChatColor.WHITE + "3 Sekunden"
                + ChatColor.GRAY + " • Nicht bewegen und keinen Schaden bekommen.");

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                PendingTeleport removed = pending.remove(player.getUniqueId());
                if (removed == null || !player.isOnline()) return;
                if (combatTagService.isTagged(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Teleport abgebrochen: Du bist jetzt im Kampf.");
                    return;
                }
                player.teleport(target);
                player.sendMessage(ChatColor.GREEN + "Du wurdest zum Spawn teleportiert.");
            }
        }, TELEPORT_DELAY_TICKS);
        pending.put(uuid, new PendingTeleport(origin, task));
        return true;
    }

    private boolean handleSetSpawn(Player player) {
        if (!player.hasPermission("skykings.admin.map")) {
            player.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        setSpawn(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Globaler SkyKings-Spawn und Worldspawn wurden gesetzt.");
        return true;
    }

    public synchronized void setSpawn(Location location) {
        if (location == null || location.getWorld() == null) return;
        this.spawn = location.clone();
        location.getWorld().setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        save();
    }

    public Location getSpawn() {
        Location current = spawn;
        return current == null ? null : current.clone();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        PendingTeleport teleport = pending.get(event.getPlayer().getUniqueId());
        if (teleport == null || event.getTo() == null) return;
        Location from = teleport.origin;
        Location to = event.getTo();
        if (from.getWorld() != to.getWorld()
                || from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            cancel(event.getPlayer(), "Teleport abgebrochen: Du hast dich bewegt.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) cancel((Player) event.getEntity(), "Teleport abgebrochen: Du hast Schaden bekommen.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer(), null);
    }

    public void shutdown() {
        for (PendingTeleport teleport : pending.values()) teleport.task.cancel();
        pending.clear();
    }

    private void cancel(Player player, String message) {
        PendingTeleport teleport = pending.remove(player.getUniqueId());
        if (teleport == null) return;
        teleport.task.cancel();
        if (message != null) player.sendMessage(ChatColor.RED + message);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String worldName = yaml.getString("world");
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        spawn = new Location(world,
                yaml.getDouble("x"), yaml.getDouble("y"), yaml.getDouble("z"),
                (float) yaml.getDouble("yaw"), (float) yaml.getDouble("pitch"));
    }

    private synchronized void save() {
        Location current = spawn;
        if (current == null || current.getWorld() == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("world", current.getWorld().getName());
        yaml.set("x", current.getX());
        yaml.set("y", current.getY());
        yaml.set("z", current.getZ());
        yaml.set("yaw", current.getYaw());
        yaml.set("pitch", current.getPitch());
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Spawn konnte nicht gespeichert werden.", ex);
        }
    }
}
