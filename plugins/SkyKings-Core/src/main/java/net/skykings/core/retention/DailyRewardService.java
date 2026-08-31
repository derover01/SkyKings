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

    public DailyRewardService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "daily-rewards.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean claim(Player player) {
        UUID uuid = player.getUniqueId();
        int today = dayId();
        int last = data.getInt("players." + uuid + ".last-day", -1);
        if (last == today) return false;
        int streak = data.getInt("players." + uuid + ".streak", 0);
        if (last == yesterdayId()) streak++; else streak = 1;
        if (streak > 7) streak = 1;

        long coins = 50_000L + (streak - 1L) * 25_000L;
        int stars = streak >= 7 ? 3 : (streak >= 4 ? 2 : 1);
        economy.deposit(uuid, coins, "DAILY_REWARD", "Daily Tag " + streak);
        Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, stars));
        for (ItemStack stack : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), stack);
        data.set("players." + uuid + ".last-day", today);
        data.set("players." + uuid + ".streak", streak);
        save();
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "DAILY REWARD " + ChatColor.YELLOW + "Tag " + streak
                + ChatColor.GRAY + " - +" + coins + " Coins, +" + stars + " Netherstern" + (stars == 1 ? "" : "e"));
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.75F, 1.4F);
        return true;
    }

    public int getStreak(UUID uuid) { return data.getInt("players." + uuid + ".streak", 0); }

    public long secondsUntilNext(UUID uuid) {
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

    public void save() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("daily-rewards.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
