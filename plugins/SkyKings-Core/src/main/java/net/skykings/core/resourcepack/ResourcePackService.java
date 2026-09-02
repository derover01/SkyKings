package net.skykings.core.resourcepack;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Server-seitige Auslieferung des optionalen SkyKings Resource Packs fuer Minecraft 1.8.x.
 * Gameplay bleibt voll funktionsfaehig, auch wenn der Spieler den Pack nicht nutzt.
 */
public final class ResourcePackService implements Listener, CommandExecutor {
    private static final int MAX_JOIN_DELAY_TICKS = 20 * 30;
    private static final long RESEND_COOLDOWN_MS = 10_000L;

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastRequestAt = new HashMap<UUID, Long>();

    public ResourcePackService(JavaPlugin plugin) {
        this.plugin = plugin;
        logConfigurationState();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("resource-pack.send-on-join", true)) return;
        final Player player = event.getPlayer();
        long delay = plugin.getConfig().getLong("resource-pack.join-delay-ticks", 40L);
        delay = Math.max(1L, Math.min(MAX_JOIN_DELAY_TICKS, delay));
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) send(player, false);
            }
        }, delay);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastRequestAt.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("SkyKings Resource Pack kann nur einem Spieler-Client gesendet werden.");
            return true;
        }
        send((Player) sender, true);
        return true;
    }

    public boolean send(Player player, boolean manual) {
        if (player == null) return false;
        if (!isEnabled()) {
            if (manual) player.sendMessage(ChatColor.YELLOW + "Der SkyKings Resource Pack ist aktuell noch nicht serverseitig aktiviert.");
            return false;
        }

        String url = configuredUrl();
        String error = validationError(url);
        if (error != null) {
            if (manual) player.sendMessage(ChatColor.RED + "Der SkyKings Resource Pack ist noch nicht korrekt veroeffentlicht.");
            plugin.getLogger().warning("Resource-Pack konnte nicht an " + player.getName() + " gesendet werden: " + error);
            return false;
        }

        long now = System.currentTimeMillis();
        Long previous = lastRequestAt.get(player.getUniqueId());
        long remaining = previous == null ? 0L : remainingCooldownSeconds(previous.longValue(), now);
        if (remaining > 0L) {
            if (manual) player.sendMessage(ChatColor.YELLOW + "Bitte warte noch " + remaining + "s, bevor du den Pack erneut anforderst.");
            return false;
        }

        try {
            player.setResourcePack(url);
            lastRequestAt.put(player.getUniqueId(), now);
            if (manual) {
                player.sendMessage(ChatColor.AQUA + "SkyKings Resource Pack wurde angefordert.");
                player.sendMessage(ChatColor.GRAY + "Falls nichts erscheint: Server-Ressourcenpakete in der Serverliste aktivieren.");
            }
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Resource-Pack-Anforderung fuer " + player.getName() + " fehlgeschlagen.", ex);
            if (manual) player.sendMessage(ChatColor.RED + "Resource Pack konnte nicht angefordert werden. Bitte Staff informieren.");
            return false;
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("resource-pack.enabled", false);
    }

    public String configuredUrl() {
        String raw = plugin.getConfig().getString("resource-pack.url", "");
        return raw == null ? "" : raw.trim();
    }

    void logConfigurationState() {
        if (!isEnabled()) {
            plugin.getLogger().info("SkyKings Resource Pack: serverseitige Auslieferung deaktiviert (Gameplay-Fallback aktiv).");
            return;
        }
        String error = validationError(configuredUrl());
        if (error == null) plugin.getLogger().info("SkyKings Resource Pack: HTTPS-Auslieferung aktiviert.");
        else plugin.getLogger().warning("SkyKings Resource Pack ist aktiviert, aber nicht sendbar: " + error);
    }

    /** Gibt null bei einer fuer 1.8 geeigneten HTTPS-URL zurueck, sonst einen Diagnosegrund. */
    public static String validationError(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "resource-pack.url ist leer";
        String url = raw.trim();
        if (!isAscii(url)) return "URL enthaelt Nicht-ASCII-Zeichen";
        if (url.length() > 255) return "URL ist laenger als 255 Zeichen";
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return "URL muss HTTPS verwenden";
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) return "URL enthaelt keinen Host";
            if (uri.getFragment() != null) return "URL darf kein #Fragment enthalten";
            return null;
        } catch (URISyntaxException ex) {
            return "URL-Syntax ist ungueltig: " + ex.getMessage();
        }
    }

    public static long remainingCooldownSeconds(long lastRequestMillis, long nowMillis) {
        long elapsed = Math.max(0L, nowMillis - lastRequestMillis);
        long remainingMs = RESEND_COOLDOWN_MS - elapsed;
        if (remainingMs <= 0L) return 0L;
        return (remainingMs + 999L) / 1000L;
    }

    private static boolean isAscii(String value) {
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) > 0x7F) return false;
        return true;
    }
}
