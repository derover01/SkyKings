package net.skykings.admin.warp;

import net.skykings.combat.tag.CombatTagService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Sichere Warp-Schnellreise mit Combat-Sperre und 3-Sekunden-Countdown. */
public final class WarpTeleportService implements Listener {
    private static final int COUNTDOWN_SECONDS = 3;

    private final JavaPlugin plugin;
    private final WarpService warps;
    private final CombatTagService combatTags;
    private final Map<UUID, PendingTeleport> pending = new HashMap<UUID, PendingTeleport>();

    public WarpTeleportService(JavaPlugin plugin, WarpService warps, CombatTagService combatTags) {
        this.plugin = plugin;
        this.warps = warps;
        this.combatTags = combatTags;
    }

    public void request(final Player player, final String warpName) {
        if (!warps.exists(warpName)) {
            player.sendMessage(UiTheme.DANGER + "Warp nicht gefunden: " + ChatColor.WHITE + warpName);
            SoundFeedback.error(player);
            return;
        }
        if (combatTags == null) {
            player.sendMessage(UiTheme.DANGER + "Warp ist gerade nicht verfügbar: Combat-System nicht bereit.");
            SoundFeedback.error(player);
            return;
        }
        if (combatTags.isTagged(player.getUniqueId())) {
            long seconds = Math.max(1L, (combatTags.getRemainingMillis(player.getUniqueId()) + 999L) / 1000L);
            player.sendMessage(UiTheme.DANGER + "Du bist im Combat. Warpen ist noch " + ChatColor.WHITE + seconds
                    + UiTheme.DANGER + "s gesperrt.");
            SoundFeedback.error(player);
            return;
        }
        final Location target = warps.get(warpName);
        if (target == null) {
            player.sendMessage(UiTheme.DANGER + "Die Welt dieses Warps ist aktuell nicht geladen.");
            SoundFeedback.error(player);
            return;
        }

        cancel(player, null, false);
        player.closeInventory();
        final Location origin = player.getLocation().clone();

        BukkitTask task = new BukkitRunnable() {
            private int remaining = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    clear(player.getUniqueId(), false);
                    cancel();
                    return;
                }
                if (combatTags.isTagged(player.getUniqueId())) {
                    clear(player.getUniqueId(), false);
                    player.sendMessage(UiTheme.DANGER + "Teleport abgebrochen: Du bist jetzt im Combat.");
                    SoundFeedback.error(player);
                    cancel();
                    return;
                }
                if (remaining > 0) {
                    player.sendMessage(UiTheme.WARNING + "Teleport zu " + ChatColor.WHITE + warpName
                            + UiTheme.WARNING + " in " + ChatColor.WHITE + remaining + UiTheme.WARNING + "s"
                            + ChatColor.DARK_GRAY + " • nicht bewegen");
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.6F, 1.0F + (COUNTDOWN_SECONDS - remaining) * 0.15F);
                    remaining--;
                    return;
                }

                clear(player.getUniqueId(), false);
                player.teleport(target.clone());
                player.sendMessage(UiTheme.SUCCESS + "Teleportiert zu " + ChatColor.WHITE + warpName + UiTheme.SUCCESS + ".");
                SoundFeedback.success(player);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        pending.put(player.getUniqueId(), new PendingTeleport(origin, task));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!pending.containsKey(event.getPlayer().getUniqueId()) || event.getTo() == null) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() != to.getWorld()
                || Math.abs(from.getX() - to.getX()) > 0.001D
                || Math.abs(from.getY() - to.getY()) > 0.001D
                || Math.abs(from.getZ() - to.getZ()) > 0.001D) {
            cancel(event.getPlayer(), "Teleport abgebrochen: Du hast dich bewegt.", true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (pending.containsKey(player.getUniqueId())) {
            cancel(player, "Teleport abgebrochen: Du hast Schaden bekommen.", true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        clear(event.getEntity().getUniqueId(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId(), true);
    }

    public void shutdown() {
        for (PendingTeleport value : pending.values()) value.task.cancel();
        pending.clear();
    }

    private void cancel(Player player, String message, boolean feedback) {
        PendingTeleport value = pending.remove(player.getUniqueId());
        if (value == null) return;
        value.task.cancel();
        if (message != null) player.sendMessage(UiTheme.DANGER + message);
        if (feedback) SoundFeedback.error(player);
    }

    private void clear(UUID uuid, boolean cancelTask) {
        PendingTeleport value = pending.remove(uuid);
        if (value != null && cancelTask) value.task.cancel();
    }

    private static final class PendingTeleport {
        final Location origin;
        final BukkitTask task;
        PendingTeleport(Location origin, BukkitTask task) {
            this.origin = origin;
            this.task = task;
        }
    }
}
