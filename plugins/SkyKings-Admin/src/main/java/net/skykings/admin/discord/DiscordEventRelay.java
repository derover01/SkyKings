package net.skykings.admin.discord;

import net.skykings.combat.event.KingAltarCaptureEvent;
import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Kuratierter Discord-Relay fuer wichtige, oeffentliche SkyKings-Ereignisse.
 * Absichtlich kein Kill-/Chat-Spam: nur echte Meilensteine und Map-Objectives.
 */
public final class DiscordEventRelay implements Listener {
    private final DiscordBridge discord;

    public DiscordEventRelay(DiscordBridge discord) {
        this.discord = discord;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKingCapture(KingAltarCaptureEvent event) {
        if (!discord.isConfigured("events")) return;
        discord.send("events", "👑 **" + name(event.getPlayerUuid()) + "** hat den King Altar erobert.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(SkyKingsPlayerKillEvent event) {
        if (!discord.isConfigured("events")) return;
        if (event.getAntiFarmMultiplier() < 1.0D) return;
        int streak = event.getNewKillstreak();
        if (!isMilestone(streak)) return;
        discord.send("events", "🔥 **" + name(event.getKillerUuid()) + "** ist auf einer **" + streak + "er Killstreak**!");
    }

    private boolean isMilestone(int streak) {
        return streak == 10 || streak == 25 || streak == 50 || streak == 100 || (streak > 100 && streak % 100 == 0);
    }

    private String name(java.util.UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player == null ? null : player.getName();
        return name == null || name.trim().isEmpty() ? uuid.toString().substring(0, 8) : name;
    }
}
