package net.skykings.combat.map.zone;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.item.SkyKingsCurrencyItems;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Zusaetzlicher Reward, wenn ein PvP-Sieg innerhalb derselben Hot Zone stattfindet. */
public final class HotZoneRewardListener implements Listener {
    private static final long COIN_BONUS = 25000L;
    private static final long REPEAT_COOLDOWN_MS = 600000L;

    private final HotZoneService zones;
    private final EconomyService economy;
    private final MapMasteryService mastery;
    private final Map<String, Long> repeatCooldowns = new ConcurrentHashMap<String, Long>();

    public HotZoneRewardListener(HotZoneService zones, EconomyService economy, MapMasteryService mastery) {
        this.zones = zones;
        this.economy = economy;
        this.mastery = mastery;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player winner = victim.getKiller();
        if (winner == null || winner.getUniqueId().equals(victim.getUniqueId())) return;

        String zone = zones.findZone(winner);
        if (zone == null || !zone.equals(zones.findZone(victim))) return;

        String pair = winner.getUniqueId().toString() + ":" + victim.getUniqueId().toString();
        long now = System.currentTimeMillis();
        Long blockedUntil = repeatCooldowns.get(pair);
        if (blockedUntil != null && blockedUntil > now) return;
        repeatCooldowns.put(pair, now + REPEAT_COOLDOWN_MS);

        economy.deposit(winner.getUniqueId(), COIN_BONUS, "HOT_ZONE", "Hot Zone PvP Reward: " + zone);
        SkyKingsCurrencyItems.give(winner, 1L);
        mastery.addHotZoneKill(winner.getUniqueId());

        winner.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "HOT ZONE BONUS "
                + ChatColor.YELLOW + "+25.000 Coins, +1 SkyKings Stern"
                + ChatColor.GRAY + " | " + ChatColor.WHITE + mastery.getTitle(winner.getUniqueId()));
        winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 0.55F, 1.55F);
    }
}
