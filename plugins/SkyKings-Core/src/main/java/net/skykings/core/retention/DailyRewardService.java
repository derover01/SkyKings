package net.skykings.core.retention;

import net.skykings.core.economy.EconomyService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

/** Taeglicher Login-Reward mit 7-Tage-Streak, Coins und physischen Nethersternen. */
public final class DailyRewardService {
    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration data;
    private final JackpotService jackpotService;

    public DailyRewardService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "daily-rewards.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        this.jackpotService = new JackpotService(plugin, economy);
    }

    public JackpotService getJackpotService() { return jackpotService; }

    /** Claim wird persistent reserviert, bevor irgendein Reward ausgezahlt wird. */
    public synchronized boolean claim(Player player) {
        UUID uuid = player.getUniqueId();
        int today = dayId();
        String base = "players." + uuid;
        int last = data.getInt(base + ".last-day", -1);
        if (last == today) return false;

        int previousStreak = data.getInt(base + ".streak", 0);
        int streak = last == yesterdayId() ? previousStreak + 1 : 1;
        if (streak > 7) streak = 1;

        // Erst Claim + Streak persistent reservieren. Ein Rapid-Click sieht danach sofort last-day=today.
        data.set(base + ".last-day", today);
        data.set(base + ".streak", streak);
        if (!saveNow()) {
            if (last < 0) data.set(base + ".last-day", null); else data.set(base + ".last-day", last);
            if (previousStreak <= 0) data.set(base + ".streak", null); else data.set(base + ".streak", previousStreak);
            player.sendMessage(ChatColor.RED + "Daily Reward konnte nicht sicher gespeichert werden. Bitte spaeter erneut versuchen.");
            return false;
        }

        long coins = 50_000L + (streak - 1L) * 25_000L;
        int stars = streak >= 7 ? 3 : (streak >= 4 ? 2 : 1);
        try {
            economy.deposit(uuid, coins, "DAILY_REWARD", "Daily Tag " + streak);
        } catch (RuntimeException ex) {
            // Keine Coin-Auszahlung erfolgt: Claim wieder freigeben und persistent zurueckrollen.
            if (last < 0) data.set(base + ".last-day", null); else data.set(base + ".last-day", last);
            if (previousStreak <= 0) data.set(base + ".streak", null); else data.set(base + ".streak", previousStreak);
            if (!saveNow()) {
                plugin.getLogger().severe("Daily-Reward-Claim konnte nach Economy-Fehler nicht freigegeben werden: " + uuid);
            }
            plugin.getLogger().warning("Daily-Reward-Auszahlung fehlgeschlagen fuer " + uuid + ": " + ex.getMessage());
            player.sendMessage(ChatColor.RED + "Daily Reward konnte nicht ausgezahlt werden. Der Claim wurde nicht verbraucht.");
            return false;
        }

        try {
            Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, stars));
            for (ItemStack stack : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), stack);
        } catch (RuntimeException ex) {
            // Coins wurden bereits sicher ausgezahlt: Claim bleibt verbraucht, damit kein Coin-Dupe entsteht.
            plugin.getLogger().warning("Daily-Nethersterne konnten nicht ausgegeben werden fuer " + uuid + ": " + ex.getMessage());
            player.sendMessage(ChatColor.YELLOW + "Coins wurden ausgezahlt, Nethersterne konnten nicht ausgegeben werden. Bitte Staff informieren.");
        }

        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "DAILY REWARD " + ChatColor.YELLOW + "Tag " + streak
                + ChatColor.GRAY + " - +" + coins + " Coins, +" + stars + " Netherstern" + (stars == 1 ? "" : "e"));
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.75F, 1.4F);
        return true;
    }

    public synchronized int getStreak(UUID uuid) { return data.getInt("players." + uuid + ".streak", 0); }

    public synchronized long secondsUntilNext(UUID uuid) {
        int today = dayId();
        if (data.getInt("players." + uuid + ".last-day", -1) != today) return 0L;
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        return Math.max(0L, (next.getTimeInMillis() - System.currentTimeMillis() + 999L) / 1000L);
    }

    private int dayId() { return dayId(Calendar.getInstance()); }
    private int yesterdayId() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -1);
        return dayId(c);
    }
    private int dayId(Calendar c) { return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR); }

    public synchronized void save() { saveNow(); }

    private boolean saveNow() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Daily-Reward-Datenordner konnte nicht erstellt werden.");
                return false;
            }
            data.save(file);
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().warning("daily-rewards.yml konnte nicht gespeichert werden: " + ex.getMessage());
            return false;
        }
    }
}
