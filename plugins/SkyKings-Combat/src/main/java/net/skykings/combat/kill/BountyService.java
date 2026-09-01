package net.skykings.combat.kill;

import net.skykings.combat.retention.QuestService;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Automatische Streak-Bounties plus persistente Spieler-zu-Spieler-Kopfgelder. */
public final class BountyService {
    private static final long MIN_PLAYER_BOUNTY = 10_000L;
    private static final long MAX_SINGLE_BOUNTY = 100_000_000L;
    private static volatile BountyService active;

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final NetherstarRewardDelivery rewardDelivery;
    private final File file;
    private final Map<UUID, Long> playerBounties = new HashMap<UUID, Long>();

    public BountyService(EconomyService economyService, NetherstarRewardDelivery rewardDelivery) {
        this(JavaPlugin.getProvidingPlugin(BountyService.class), economyService, rewardDelivery);
    }

    public BountyService(JavaPlugin plugin, EconomyService economyService, NetherstarRewardDelivery rewardDelivery) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.rewardDelivery = rewardDelivery;
        this.file = new File(plugin.getDataFolder(), "player-bounties.yml");
        active = this;
        load();
    }

    public static BountyService active() { return active; }
    public static long minPlayerBounty() { return MIN_PLAYER_BOUNTY; }
    public static long maxSingleBounty() { return MAX_SINGLE_BOUNTY; }

    public boolean place(Player issuer, OfflinePlayer target, long amount) {
        if (issuer == null || target == null || target.getUniqueId().equals(issuer.getUniqueId())) return false;
        if (amount < MIN_PLAYER_BOUNTY || amount > MAX_SINGLE_BOUNTY) return false;
        if (!economyService.withdraw(issuer.getUniqueId(), amount, issuer.getName(), "Kopfgeld auf " + target.getName())) return false;
        long current = getPlayerBounty(target.getUniqueId());
        playerBounties.put(target.getUniqueId(), current + amount);
        save();
        Bukkit.broadcastMessage(UiTheme.WARNING + "Kopfgeld gesetzt " + UiTheme.MUTED + "• "
                + UiTheme.TEXT + issuer.getName() + UiTheme.MUTED + " setzt " + UiTheme.TEXT + UiFormat.coins(amount)
                + UiTheme.MUTED + " auf " + UiTheme.WARNING + safeName(target));
        return true;
    }

    public long getPlayerBounty(UUID target) {
        Long value = playerBounties.get(target); return value == null ? 0L : Math.max(0L, value);
    }

    public Map<UUID, Long> getPlayerBounties() { return Collections.unmodifiableMap(new HashMap<UUID, Long>(playerBounties)); }

    public void announceStreak(Player player, int streak) {
        Bounty bounty = bountyFor(streak);
        if (!isMilestone(streak) || bounty == null) return;
        Bukkit.broadcastMessage(UiTheme.WARNING + "Bounty active");
        Bukkit.broadcastMessage(UiTheme.TEXT + player.getName() + UiTheme.MUTED + " • Streak " + UiTheme.TEXT + streak);
        Bukkit.broadcastMessage(UiTheme.MUTED + "Reward " + UiTheme.TEXT + UiFormat.number(bounty.netherstars)
                + " SkyKings Sterne  •  " + UiTheme.TEXT + UiFormat.coins(bounty.coins));
        for (Player online : Bukkit.getOnlinePlayers()) SoundFeedback.warning(online);
    }

    public void awardStreakShutdown(Player killer, Player victim, int victimStreak, double antiFarmMultiplier) {
        boolean paidAnyBounty = false;
        Bounty base = bountyFor(victimStreak);
        if (base != null && antiFarmMultiplier > 0.0D) {
            long stars = Math.round(base.netherstars * antiFarmMultiplier);
            long coins = Math.round(base.coins * antiFarmMultiplier);
            if (stars > 0L) rewardDelivery.give(killer, stars);
            if (coins > 0L) economyService.deposit(killer.getUniqueId(), coins, "BOUNTY", "Streak-Shutdown gegen " + victim.getName());
            if (stars > 0L || coins > 0L) {
                paidAnyBounty = true;
                Bukkit.broadcastMessage(UiTheme.PRIMARY + "Streak-Bounty claimed " + UiTheme.MUTED + "• "
                        + UiTheme.TEXT + killer.getName() + UiTheme.MUTED + " besiegt " + UiTheme.TEXT + victim.getName());
            }
        }

        if (antiFarmMultiplier >= 0.999D) {
            long placed = getPlayerBounty(victim.getUniqueId());
            if (placed > 0L) {
                playerBounties.remove(victim.getUniqueId());
                save();
                economyService.deposit(killer.getUniqueId(), placed, "PLAYER_BOUNTY", "Kopfgeld auf " + victim.getName());
                Bukkit.broadcastMessage(UiTheme.LEGENDARY + "Kopfgeld kassiert " + UiTheme.MUTED + "• "
                        + UiTheme.TEXT + killer.getName() + UiTheme.MUTED + " erhaelt " + UiTheme.LEGENDARY + UiFormat.coins(placed));
                SoundFeedback.reward(killer);
                paidAnyBounty = true;
            }
        }

        // Rewards duerfen bei Anti-Farm reduziert bleiben; Questfortschritt gibt es nur fuer einen vollen legitimen Kill.
        if (paidAnyBounty && antiFarmMultiplier >= 0.999D) {
            QuestService quests = QuestService.active();
            if (quests != null) quests.recordBountyClaim(killer);
        }
    }

    public static long getCoinBounty(int streak) { Bounty bounty = bountyForStatic(streak); return bounty == null ? 0L : bounty.coins; }
    public static long getStarBounty(int streak) { Bounty bounty = bountyForStatic(streak); return bounty == null ? 0L : bounty.netherstars; }

    private boolean isMilestone(int streak) { return streak == 5 || streak == 10 || streak == 20 || streak == 30 || streak == 50 || streak == 100; }
    private Bounty bountyFor(int streak) { return bountyForStatic(streak); }
    private static Bounty bountyForStatic(int streak) {
        if (streak >= 100) return new Bounty(250L, 5000000L);
        if (streak >= 50) return new Bounty(100L, 1000000L);
        if (streak >= 30) return new Bounty(50L, 500000L);
        if (streak >= 20) return new Bounty(25L, 250000L);
        if (streak >= 10) return new Bounty(10L, 75000L);
        if (streak >= 5) return new Bounty(3L, 25000L);
        return null;
    }

    private String safeName(OfflinePlayer player) { return player.getName() == null ? player.getUniqueId().toString().substring(0, 8) : player.getName(); }

    private void load() {
        playerBounties.clear(); if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("bounties") == null) return;
        for (String raw : yaml.getConfigurationSection("bounties").getKeys(false)) {
            try { playerBounties.put(UUID.fromString(raw), Math.max(0L, yaml.getLong("bounties." + raw))); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : playerBounties.entrySet()) yaml.set("bounties." + entry.getKey(), entry.getValue());
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); yaml.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("player-bounties.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }

    private static final class Bounty {
        private final long netherstars; private final long coins;
        private Bounty(long netherstars, long coins) { this.netherstars = netherstars; this.coins = coins; }
    }
}
