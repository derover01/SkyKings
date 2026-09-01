package net.skykings.combat.retention;

import net.skykings.combat.event.EventParticipationService;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.item.SkyKingsCurrencyItems;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Daily/Weekly Quests mit Free-/Premium-Pool und direkter Season-XP-Anbindung. */
public final class QuestService implements Listener {
    private static final long SAME_VICTIM_COOLDOWN = 10L * 60L * 1000L;
    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Long> pairCooldowns = new HashMap<String, Long>();
    private final Map<UUID, Integer> liveStreaks = new HashMap<UUID, Integer>();

    public QuestService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "quests.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    @EventHandler(ignoreCancelled = true)
    public void onKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        liveStreaks.put(victim.getUniqueId(), 0);
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) return;
        if (EventParticipationService.global().isInEvent(victim.getUniqueId())
                || EventParticipationService.global().isInEvent(killer.getUniqueId())) return;
        String pair = killer.getUniqueId() + ":" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long until = pairCooldowns.get(pair);
        if (until != null && until > now) return;
        pairCooldowns.put(pair, now + SAME_VICTIM_COOLDOWN);

        prepare(killer.getUniqueId());
        add(killer, "daily.kills", 1);
        add(killer, "weekly.kills", 1);
        int streak = liveStreaks.containsKey(killer.getUniqueId()) ? liveStreaks.get(killer.getUniqueId()) + 1 : 1;
        liveStreaks.put(killer.getUniqueId(), streak);
        max(killer, "daily.streak", streak);
        if (isPremium(killer.getUniqueId())) {
            add(killer, "premium.daily.kills", 1);
            add(killer, "premium.weekly.kills", 1);
        }
        checkRewards(killer);
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
            liveStreaks.put(uuid, 0);
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
        if (get(uuid, "weekly.kills") >= 30 && !claimed(uuid, "weekly.claimed-kills"))
            reward(player, "weekly.claimed-kills", 500_000L, 5, 2_000, "Weekly: 30 PvP-Kills");

        if (!isPremium(uuid)) return;
        if (get(uuid, "premium.daily.kills") >= 10 && !claimed(uuid, "premium.daily.claimed-kills"))
            reward(player, "premium.daily.claimed-kills", 300_000L, 4, 800, "Premium Daily: 10 PvP-Kills");
        if (get(uuid, "premium.daily.pearls") >= 40 && !claimed(uuid, "premium.daily.claimed-pearls"))
            reward(player, "premium.daily.claimed-pearls", 150_000L, 3, 600, "Premium Daily: 40 Enderperlen");
        if (get(uuid, "premium.weekly.kills") >= 75 && !claimed(uuid, "premium.weekly.claimed-kills"))
            reward(player, "premium.weekly.claimed-kills", 1_250_000L, 12, 3_500, "Premium Weekly: 75 PvP-Kills");
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
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("quests.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }
}
