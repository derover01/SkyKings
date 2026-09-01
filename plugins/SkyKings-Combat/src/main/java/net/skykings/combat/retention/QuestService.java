package net.skykings.combat.retention;

import net.skykings.combat.event.EventParticipationService;
import net.skykings.combat.event.KingAltarCaptureEvent;
import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.event.CrateOpenedEvent;
import net.skykings.core.item.SkyKingsCurrencyItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

/** Daily/Weekly Quests mit Free-/Premium-Pool und direkter Season-XP-Anbindung. */
public final class QuestService implements Listener {
    private static volatile QuestService ACTIVE;

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration data;

    public QuestService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "quests.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        ACTIVE = this;
    }

    public static QuestService active() { return ACTIVE; }

    /** Nur volle, validierte SkyKings-Kills duerfen PvP-Quests fortschreiben. */
    @EventHandler
    public void onKill(SkyKingsPlayerKillEvent event) {
        if (event.getAntiFarmMultiplier() < 1.0D) return;
        Player killer = Bukkit.getPlayer(event.getKillerUuid());
        if (killer == null) return;
        prepare(killer.getUniqueId());
        add(killer, "daily.kills", 1);
        add(killer, "weekly.kills", 1);
        max(killer, "daily.streak", event.getNewKillstreak());
        if (isPremium(killer.getUniqueId())) {
            add(killer, "premium.daily.kills", 1);
            add(killer, "premium.weekly.kills", 1);
        }
        checkRewards(killer);
    }

    @EventHandler
    public void onKingAltar(KingAltarCaptureEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player == null) return;
        prepare(player.getUniqueId());
        add(player, "weekly.altar", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.altar", 1);
        checkRewards(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        if (EventParticipationService.global().isInEvent(player.getUniqueId())) return;
        prepare(player.getUniqueId());
        add(player, "daily.pearls", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.daily.pearls", 1);
        checkRewards(player);
    }

    @EventHandler
    public void onCrateOpened(CrateOpenedEvent event) {
        if (event.getPlayer() == null) return;
        Player player = event.getPlayer();
        prepare(player.getUniqueId());
        add(player, "weekly.crates", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.crates", 1);
        checkRewards(player);
    }

    /**
     * QuestService ist vor DuelService registriert und liest deshalb den noch aktiven Duel-Sessionzustand.
     * So zaehlen sowohl normale Duel-Tode als auch Forfeit/Disconnect ohne Kopplung an private DuelService-Daten.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDuelDeath(PlayerDeathEvent event) {
        recordDuelOpponentWin(event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDuelQuit(PlayerQuitEvent event) {
        recordDuelOpponentWin(event.getPlayer());
    }

    private void recordDuelOpponentWin(Player loser) {
        if (loser == null) return;
        EventParticipationService runtime = EventParticipationService.global();
        EventParticipationService.Participation loserState = runtime.get(loser.getUniqueId());
        if (loserState == null || loserState.getType() != EventParticipationService.Type.DUEL) return;

        for (Map.Entry<UUID, EventParticipationService.Participation> entry : runtime.snapshot().entrySet()) {
            if (entry.getKey().equals(loser.getUniqueId())) continue;
            EventParticipationService.Participation other = entry.getValue();
            if (other == null || other.getType() != EventParticipationService.Type.DUEL) continue;
            if (!loserState.getSessionId().equals(other.getSessionId())) continue;
            Player winner = Bukkit.getPlayer(entry.getKey());
            if (winner != null && winner.isOnline()) recordDuelWin(winner);
            return;
        }
    }

    /** Wird nur fuer einen tatsaechlichen Duel-Sieg aufgerufen. */
    public void recordDuelWin(Player player) {
        if (player == null) return;
        prepare(player.getUniqueId());
        add(player, "daily.duels", 1);
        add(player, "weekly.duels", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.duels", 1);
        checkRewards(player);
    }

    /** Pro Kill maximal einmal aufrufen, auch wenn Streak- und Player-Bounty gleichzeitig ausbezahlt werden. */
    public void recordBountyClaim(Player player) {
        if (player == null) return;
        prepare(player.getUniqueId());
        add(player, "weekly.bounties", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.bounties", 1);
        checkRewards(player);
    }

    /** Nur wenn eine Rare/Epic Map-Loot-Chest tatsaechlich frisch geoeffnet/refilled wurde. */
    public void recordRareMapChest(Player player) {
        if (player == null) return;
        prepare(player.getUniqueId());
        add(player, "weekly.rare-chests", 1);
        if (isPremium(player.getUniqueId())) add(player, "premium.weekly.rare-chests", 1);
        checkRewards(player);
    }

    public boolean isPremium(UUID uuid) {
        BattlePassService service = BattlePassService.active();
        return service != null && service.isPremium(uuid);
    }

    public void prepare(UUID uuid) {
        long day = dayId();
        int week = weekId();
        String p = "players." + uuid + ".";
        boolean changed = false;
        if (data.getLong(p + "daily.id", -1L) != day) {
            data.set(p + "daily", null);
            data.set(p + "daily.id", day);
            data.set(p + "premium.daily", null);
            changed = true;
        }
        if (data.getInt(p + "weekly.id", -1) != week) {
            data.set(p + "weekly", null);
            data.set(p + "weekly.id", week);
            data.set(p + "premium.weekly", null);
            changed = true;
        }
        if (changed) save();
    }

    public int get(UUID uuid, String key) { prepare(uuid); return data.getInt("players." + uuid + "." + key, 0); }
    public boolean claimed(UUID uuid, String key) { prepare(uuid); return data.getBoolean("players." + uuid + "." + key, false); }

    private void add(Player player, String key, int amount) {
        String p = "players." + player.getUniqueId() + "." + key;
        data.set(p, data.getInt(p, 0) + amount);
        save();
    }

    private void max(Player player, String key, int value) {
        String p = "players." + player.getUniqueId() + "." + key;
        if (value > data.getInt(p, 0)) {
            data.set(p, value);
            save();
        }
    }

    private void checkRewards(Player player) {
        UUID uuid = player.getUniqueId();
        if (get(uuid, "daily.kills") >= 5 && !claimed(uuid, "daily.claimed-kills"))
            reward(player, "daily.claimed-kills", 150_000L, 2, 500, "Daily: 5 PvP-Kills");
        if (get(uuid, "daily.pearls") >= 20 && !claimed(uuid, "daily.claimed-pearls"))
            reward(player, "daily.claimed-pearls", 75_000L, 1, 350, "Daily: 20 Enderperlen");
        if (get(uuid, "daily.streak") >= 5 && !claimed(uuid, "daily.claimed-streak"))
            reward(player, "daily.claimed-streak", 200_000L, 3, 750, "Daily: 5er Killstreak");
        if (get(uuid, "daily.duels") >= 1 && !claimed(uuid, "daily.claimed-duels"))
            reward(player, "daily.claimed-duels", 125_000L, 2, 600, "Daily: 1 Duel-Sieg");

        if (get(uuid, "weekly.kills") >= 30 && !claimed(uuid, "weekly.claimed-kills"))
            reward(player, "weekly.claimed-kills", 500_000L, 5, 2_000, "Weekly: 30 PvP-Kills");
        if (get(uuid, "weekly.altar") >= 3 && !claimed(uuid, "weekly.claimed-altar"))
            reward(player, "weekly.claimed-altar", 350_000L, 4, 1_500, "Weekly: 3 King-Altar Captures");
        if (get(uuid, "weekly.crates") >= 3 && !claimed(uuid, "weekly.claimed-crates"))
            reward(player, "weekly.claimed-crates", 300_000L, 4, 1_500, "Weekly: 3 Crates oeffnen");
        if (get(uuid, "weekly.bounties") >= 1 && !claimed(uuid, "weekly.claimed-bounties"))
            reward(player, "weekly.claimed-bounties", 350_000L, 5, 1_750, "Weekly: 1 Bounty kassieren");
        if (get(uuid, "weekly.rare-chests") >= 2 && !claimed(uuid, "weekly.claimed-rare-chests"))
            reward(player, "weekly.claimed-rare-chests", 250_000L, 4, 1_500, "Weekly: 2 Rare Map Chests");

        if (!isPremium(uuid)) return;
        if (get(uuid, "premium.daily.kills") >= 10 && !claimed(uuid, "premium.daily.claimed-kills"))
            reward(player, "premium.daily.claimed-kills", 300_000L, 4, 800, "Premium Daily: 10 PvP-Kills");
        if (get(uuid, "premium.daily.pearls") >= 40 && !claimed(uuid, "premium.daily.claimed-pearls"))
            reward(player, "premium.daily.claimed-pearls", 150_000L, 3, 600, "Premium Daily: 40 Enderperlen");
        if (get(uuid, "premium.weekly.kills") >= 75 && !claimed(uuid, "premium.weekly.claimed-kills"))
            reward(player, "premium.weekly.claimed-kills", 1_250_000L, 12, 3_500, "Premium Weekly: 75 PvP-Kills");
        if (get(uuid, "premium.weekly.altar") >= 7 && !claimed(uuid, "premium.weekly.claimed-altar"))
            reward(player, "premium.weekly.claimed-altar", 750_000L, 8, 2_500, "Premium Weekly: 7 King-Altar Captures");
        if (get(uuid, "premium.weekly.duels") >= 3 && !claimed(uuid, "premium.weekly.claimed-duels"))
            reward(player, "premium.weekly.claimed-duels", 600_000L, 7, 2_250, "Premium Weekly: 3 Duel-Siege");
        if (get(uuid, "premium.weekly.bounties") >= 2 && !claimed(uuid, "premium.weekly.claimed-bounties"))
            reward(player, "premium.weekly.claimed-bounties", 700_000L, 8, 2_500, "Premium Weekly: 2 Bounties");
        if (get(uuid, "premium.weekly.crates") >= 5 && !claimed(uuid, "premium.weekly.claimed-crates"))
            reward(player, "premium.weekly.claimed-crates", 500_000L, 6, 2_000, "Premium Weekly: 5 Crates");
        if (get(uuid, "premium.weekly.rare-chests") >= 4 && !claimed(uuid, "premium.weekly.claimed-rare-chests"))
            reward(player, "premium.weekly.claimed-rare-chests", 500_000L, 6, 2_000, "Premium Weekly: 4 Rare Map Chests");
    }

    private void reward(Player player, String claimedKey, long coins, int stars, int seasonXp, String name) {
        UUID uuid = player.getUniqueId();
        data.set("players." + uuid + "." + claimedKey, true);
        economy.deposit(uuid, coins, "QUEST_REWARD", name);
        SkyKingsCurrencyItems.give(player, stars);
        SeasonProgressService progress = SeasonProgressService.active();
        if (progress != null) progress.addXp(player, seasonXp, "Quest: " + name);
        save();
        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "QUEST ABGESCHLOSSEN " + ChatColor.YELLOW + name
                + ChatColor.GRAY + " • +" + coins + " Coins • +" + stars + " Sterne • +" + seasonXp + " Season-XP");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7F, 1.5F);
    }

    private long dayId() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR);
    }

    private int weekId() {
        Calendar c = Calendar.getInstance();
        return c.getWeekYear() * 100 + c.get(Calendar.WEEK_OF_YEAR);
    }

    public void save() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("quests.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
