package net.skykings.combat.map.zone;

import net.skykings.core.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * King Altar / KOTH: ein einzelner Spieler muss die Zone 60 Sekunden lang kontrollieren.
 * Bei mehreren Spielern pausiert der Fortschritt. Nach Capture folgt Reward + Cooldown.
 */
public final class KingAltarService {
    private static final int CAPTURE_SECONDS = 60;
    private static final int COOLDOWN_SECONDS = 300;
    private static final long COIN_REWARD = 250_000L;
    private static final int STAR_REWARD = 10;

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final File file;
    private MapZone zone;
    private UUID capturing;
    private int progress;
    private int cooldown;

    public KingAltarService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "king-altar.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public MapZone getZone() { return zone; }
    public int getProgress() { return progress; }
    public int getCooldown() { return cooldown; }
    public UUID getCapturing() { return capturing; }

    public void setZone(Player player, double radius) {
        zone = new MapZone("king-altar", player.getWorld().getName(), player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(), radius);
        resetRound();
        save();
    }

    public void removeZone() {
        zone = null;
        resetRound();
        save();
    }

    public boolean isInside(Player player) {
        return zone != null && zone.contains(player.getLocation());
    }

    private void tick() {
        if (zone == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        List<Player> inside = new ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isDead() && zone.contains(player.getLocation())) inside.add(player);
        }

        if (inside.size() != 1) {
            if (inside.size() > 1 && capturing != null && progress > 0 && progress % 10 == 0) {
                for (Player player : inside) {
                    player.sendMessage(ChatColor.RED + "King Altar umkämpft - Capture pausiert!");
                    player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.5F, 0.8F);
                }
            }
            capturing = null;
            progress = 0;
            return;
        }

        Player player = inside.get(0);
        if (!player.getUniqueId().equals(capturing)) {
            capturing = player.getUniqueId();
            progress = 0;
            player.sendMessage(ChatColor.GOLD + "Du kontrollierst den King Altar. Halte ihn " + CAPTURE_SECONDS + " Sekunden!");
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.8F, 1.0F);
        }

        progress++;
        int remaining = Math.max(0, CAPTURE_SECONDS - progress);
        if (remaining == 30 || remaining == 15 || remaining <= 5) {
            player.sendMessage(ChatColor.GOLD + "King Altar: " + ChatColor.YELLOW + remaining + " Sekunden verbleibend.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.45F, 1.25F);
        }
        if (progress >= CAPTURE_SECONDS) capture(player);
    }

    private void capture(Player player) {
        economy.deposit(player.getUniqueId(), COIN_REWARD, "KING_ALTAR", "King Altar Capture");
        ItemStack stars = new ItemStack(Material.NETHER_STAR, STAR_REWARD);
        java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(stars);
        for (ItemStack item : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
        player.updateInventory();

        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "KING ALTAR " + ChatColor.YELLOW
                + player.getName() + ChatColor.GOLD + " hat den Altar erobert! " + ChatColor.GRAY
                + "(+" + COIN_REWARD + " Coins, +" + STAR_REWARD + " Nethersterne)");
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.ENDERDRAGON_GROWL, 0.55F, 1.25F);
        }
        capturing = null;
        progress = 0;
        cooldown = COOLDOWN_SECONDS;
    }

    private void resetRound() {
        capturing = null;
        progress = 0;
        cooldown = 0;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String world = yaml.getString("zone.world");
        if (world == null || world.trim().isEmpty()) return;
        zone = new MapZone("king-altar", world,
                yaml.getDouble("zone.x"), yaml.getDouble("zone.y"), yaml.getDouble("zone.z"),
                yaml.getDouble("zone.radius", 8D));
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        if (zone != null) {
            yaml.set("zone.world", zone.getWorld());
            yaml.set("zone.x", zone.getX());
            yaml.set("zone.y", zone.getY());
            yaml.set("zone.z", zone.getZ());
            yaml.set("zone.radius", zone.getRadius());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("king-altar.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
