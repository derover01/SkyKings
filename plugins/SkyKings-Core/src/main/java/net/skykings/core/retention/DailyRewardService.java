package net.skykings.core.retention;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.transaction.GameplaySettlementJournal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Taeglicher Login-Reward mit 7-Tage-Streak, Coins und physischen Nethersternen. */
public final class DailyRewardService {
    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final GameplaySettlementJournal settlementJournal;
    private final File file;
    private final YamlConfiguration data;
    private final JackpotService jackpotService;

    public DailyRewardService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        GameplaySettlementJournal existing = GameplaySettlementJournal.active();
        this.settlementJournal = existing != null ? existing : new GameplaySettlementJournal(plugin);
        this.file = new File(plugin.getDataFolder(), "daily-rewards.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        this.jackpotService = new JackpotService(plugin, economy);
    }

    public JackpotService getJackpotService() { return jackpotService; }

    /** Claim + Coins + physische Sterne werden als eine fail-closed Gameplay-Transaktion behandelt. */
    public synchronized boolean claim(Player player) {
        UUID uuid = player.getUniqueId();
        if (settlementJournal.hasPendingFor(uuid)) {
            reviewMessage(player);
            return false;
        }

        int today = dayId();
        String base = "players." + uuid;
        int last = data.getInt(base + ".last-day", -1);
        if (last == today) return false;

        int previousStreak = data.getInt(base + ".streak", 0);
        int streak = last == yesterdayId() ? previousStreak + 1 : 1;
        if (streak > 7) streak = 1;

        long coins = 50_000L + (streak - 1L) * 25_000L;
        int stars = streak >= 7 ? 3 : (streak >= 4 ? 2 : 1);
        ItemStack starReward = new ItemStack(Material.NETHER_STAR, stars);
        if (!canFit(player, starReward)) {
            player.sendMessage(ChatColor.RED + "Du brauchst Inventarplatz fuer deinen Daily Reward.");
            return false;
        }

        UUID transaction = settlementJournal.begin(uuid, "DAILY_REWARD", String.valueOf(today),
                "streak=" + streak + ", coins=" + coins + ", stars=" + stars);
        if (transaction == null) {
            player.sendMessage(ChatColor.RED + "Daily Reward konnte nicht sicher vorbereitet werden.");
            return false;
        }

        // Claim + Streak persistent reservieren, bevor irgendein Reward ausgezahlt wird.
        data.set(base + ".last-day", today);
        data.set(base + ".streak", streak);
        if (!saveNow()) {
            restoreClaim(base, last, previousStreak);
            closeOrReview(transaction, player, "DAILY_CLAIM_NOT_COMMITTED_JOURNAL_CLOSE_FAILED");
            player.sendMessage(ChatColor.RED + "Daily Reward konnte nicht sicher gespeichert werden. Bitte spaeter erneut versuchen.");
            return false;
        }

        try {
            economy.deposit(uuid, coins, "DAILY_REWARD", "Daily Tag " + streak);
        } catch (RuntimeException ex) {
            // Nach Beginn der Reward-Mutation nicht mehr raten, ob eine Teilmutation stattgefunden hat.
            settlementJournal.noteFailure(transaction, "COIN_MUTATION_FAILED_AFTER_DAILY_CLAIM_COMMIT");
            plugin.getLogger().log(Level.SEVERE, "Daily-Coin-Auszahlung hat einen unklaren Zustand erreicht: " + uuid, ex);
            reviewMessage(player);
            return false;
        }

        if (!economy.persistNow(uuid)) {
            settlementJournal.noteFailure(transaction, "DAILY_COIN_DURABLE_COMMIT_FAILED");
            reviewMessage(player);
            return false;
        }

        Map<Integer, ItemStack> left = player.getInventory().addItem(starReward);
        if (left != null && !left.isEmpty()) {
            settlementJournal.noteFailure(transaction, "DAILY_STAR_DELIVERY_PARTIAL_AFTER_COIN_COMMIT");
            reviewMessage(player);
            return false;
        }
        player.updateInventory();
        if (!savePlayerData(player)) {
            settlementJournal.noteFailure(transaction, "DAILY_PLAYERDATA_COMMIT_FAILED_AFTER_COIN_COMMIT");
            reviewMessage(player);
            return false;
        }

        if (!closeOrReview(transaction, player, "DAILY_COMMITTED_BUT_JOURNAL_CLOSE_FAILED")) return false;

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

    private void restoreClaim(String base, int last, int previousStreak) {
        if (last < 0) data.set(base + ".last-day", null); else data.set(base + ".last-day", last);
        if (previousStreak <= 0) data.set(base + ".streak", null); else data.set(base + ".streak", previousStreak);
    }

    private boolean canFit(Player player, ItemStack reward) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        return temp.addItem(reward.clone()).isEmpty();
    }

    private boolean savePlayerData(Player player) {
        try {
            player.saveData();
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Daily-Spielerdaten konnten nicht synchron gespeichert werden: " + player.getUniqueId(), ex);
            return false;
        }
    }

    private boolean closeOrReview(UUID transaction, Player player, String reason) {
        if (settlementJournal.complete(transaction)) return true;
        settlementJournal.noteFailure(transaction, reason);
        reviewMessage(player);
        return false;
    }

    private void reviewMessage(Player player) {
        player.sendMessage(ChatColor.RED + "Eine Gameplay-Transaktion hat einen unklaren Speicherzustand. Bitte Staff informieren.");
        player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.5F, 0.7F);
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
        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Daily-Reward-Datenordner konnte nicht erstellt werden.");
                return false;
            }
            data.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "daily-rewards.yml konnte nicht atomar gespeichert werden.", ex);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }
}
