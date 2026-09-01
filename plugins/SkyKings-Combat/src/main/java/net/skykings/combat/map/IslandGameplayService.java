package net.skykings.combat.map;

import net.skykings.combat.map.zone.MapMasteryService;
import net.skykings.core.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Risk/Reward-Loops und persistentes Landmark-Mastery fuer die vier Map-Islands. */
public final class IslandGameplayService {
    private static final int GOLD_HOLD_SECONDS = 300;
    private static final int LEVEL_HOLD_SECONDS = 180;
    private static final long GOLD_COIN_REWARD = 75000L;
    private static final int GOLD_INGOT_REWARD = 3;
    private static final int LEVEL_XP_REWARD = 180;
    private static final int MASTERY_SAVE_INTERVAL_SECONDS = 60;

    private final JavaPlugin plugin;
    private final MapLandmarkService landmarks;
    private final EconomyService economy;
    private final MapMasteryService mastery;
    private final Map<UUID, Integer> goldProgress = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> levelProgress = new HashMap<UUID, Integer>();
    private final Map<UUID, MapLandmarkService.Type> lastLandmark = new HashMap<UUID, MapLandmarkService.Type>();
    private int secondsSinceMasterySave;

    public IslandGameplayService(JavaPlugin plugin, MapLandmarkService landmarks, EconomyService economy,
                                 MapMasteryService mastery) {
        this.plugin = plugin;
        this.landmarks = landmarks;
        this.economy = economy;
        this.mastery = mastery;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickMastery(player);
            tickGold(player);
            tickLevel(player);
        }
        goldProgress.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        levelProgress.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        lastLandmark.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        secondsSinceMasterySave++;
        if (secondsSinceMasterySave >= MASTERY_SAVE_INTERVAL_SECONDS) {
            mastery.save();
            secondsSinceMasterySave = 0;
        }
    }

    private void tickMastery(Player player) {
        UUID uuid = player.getUniqueId();
        MapLandmarkService.Type type = player.isDead() ? null : landmarks.getTypeAt(player.getLocation());
        MapLandmarkService.Type previous = lastLandmark.get(uuid);

        if (type == null) {
            lastLandmark.remove(uuid);
            return;
        }

        mastery.addLandmarkSecond(uuid, type);
        if (previous != type) mastery.addLandmarkVisit(uuid, type);
        lastLandmark.put(uuid, type);
    }

    private void tickGold(Player player) {
        UUID uuid = player.getUniqueId();
        if (!landmarks.isInside(MapLandmarkService.Type.GOLD, player.getLocation()) || player.isDead()) {
            goldProgress.remove(uuid);
            return;
        }
        int progress = goldProgress.containsKey(uuid) ? goldProgress.get(uuid) + 1 : 1;
        if (progress == 1) {
            player.sendMessage(ChatColor.GOLD + "Gold Island: Halte die Insel 5 Minuten fuer einen Ressourcen-Reward.");
        }
        if (progress == 240) player.sendMessage(ChatColor.YELLOW + "Gold Island: noch 60 Sekunden bis zum Reward.");
        if (progress >= GOLD_HOLD_SECONDS) {
            economy.deposit(uuid, GOLD_COIN_REWARD, "GOLD_ISLAND", "Gold Island hold reward");
            java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, GOLD_INGOT_REWARD));
            for (ItemStack item : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
            mastery.addLandmarkActivity(uuid, MapLandmarkService.Type.GOLD);
            player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "GOLD ISLAND REWARD! " + ChatColor.YELLOW
                    + "+75.000 Coins +3 Goldbarren");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.65F, 1.45F);
            progress = 0;
        }
        goldProgress.put(uuid, progress);
    }

    private void tickLevel(Player player) {
        UUID uuid = player.getUniqueId();
        if (!landmarks.isInside(MapLandmarkService.Type.LEVEL, player.getLocation()) || player.isDead()) {
            levelProgress.remove(uuid);
            return;
        }
        int progress = levelProgress.containsKey(uuid) ? levelProgress.get(uuid) + 1 : 1;
        if (progress == 1) {
            player.sendMessage(ChatColor.AQUA + "Level Island: Bleibe 3 Minuten hier fuer einen XP-Schub.");
        }
        if (progress == 150) player.sendMessage(ChatColor.AQUA + "Level Island: noch 30 Sekunden bis zum XP-Reward.");
        if (progress >= LEVEL_HOLD_SECONDS) {
            player.giveExp(LEVEL_XP_REWARD);
            mastery.addLandmarkActivity(uuid, MapLandmarkService.Type.LEVEL);
            player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "LEVEL ISLAND REWARD! " + ChatColor.YELLOW
                    + "+" + LEVEL_XP_REWARD + " XP");
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8F, 1.35F);
            progress = 0;
        }
        levelProgress.put(uuid, progress);
    }

    public void shutdown() {
        mastery.save();
        goldProgress.clear();
        levelProgress.clear();
        lastLandmark.clear();
    }
}
