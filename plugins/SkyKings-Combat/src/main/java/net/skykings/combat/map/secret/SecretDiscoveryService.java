package net.skykings.combat.map.secret;

import net.skykings.combat.map.zone.MapMasteryService;
import net.skykings.combat.map.zone.MapZone;
import net.skykings.core.economy.EconomyService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Einmalige, persistente Secret-Entdeckungen auf der SkyPvP-Map. */
public final class SecretDiscoveryService implements Listener {
    private static final long COIN_REWARD = 50_000L;
    private static final int STAR_REWARD = 1;

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final MapMasteryService mastery;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, MapZone> secrets = new LinkedHashMap<String, MapZone>();

    public SecretDiscoveryService(JavaPlugin plugin, EconomyService economy, MapMasteryService mastery) {
        this.plugin = plugin;
        this.economy = economy;
        this.mastery = mastery;
        this.file = new File(plugin.getDataFolder(), "map-secrets.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        loadSecrets();
    }

    public Map<String, MapZone> getSecrets() { return new LinkedHashMap<String, MapZone>(secrets); }

    public void add(String id, Player player, double radius) {
        String key = normalize(id);
        secrets.put(key, new MapZone(key, player.getWorld().getName(), player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(), radius));
        save();
    }

    public boolean remove(String id) {
        boolean removed = secrets.remove(normalize(id)) != null;
        if (removed) save();
        return removed;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Player player = event.getPlayer();
        for (Map.Entry<String, MapZone> entry : secrets.entrySet()) {
            if (!entry.getValue().contains(player.getLocation())) continue;
            String id = entry.getKey();
            String path = "players." + player.getUniqueId() + ".found." + id;
            if (data.getBoolean(path, false)) return;
            data.set(path, true);
            economy.deposit(player.getUniqueId(), COIN_REWARD, "MAP_SECRET", "Secret " + id);
            java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, STAR_REWARD));
            for (ItemStack stack : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), stack);
            mastery.addSecret(player.getUniqueId());
            save();
            player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SECRET GEFUNDEN " + ChatColor.WHITE + id
                    + ChatColor.GRAY + "  +" + COIN_REWARD + " Coins, +" + STAR_REWARD + " Netherstern");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7F, 1.7F);
            return;
        }
    }

    private void loadSecrets() {
        if (data.getConfigurationSection("secrets") == null) return;
        for (String key : data.getConfigurationSection("secrets").getKeys(false)) {
            String base = "secrets." + key;
            String world = data.getString(base + ".world");
            if (world == null) continue;
            secrets.put(key, new MapZone(key, world, data.getDouble(base + ".x"), data.getDouble(base + ".y"),
                    data.getDouble(base + ".z"), data.getDouble(base + ".radius", 2D)));
        }
    }

    private String normalize(String raw) {
        return raw == null ? "secret" : raw.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
    }

    public void save() {
        data.set("secrets", null);
        for (Map.Entry<String, MapZone> entry : secrets.entrySet()) {
            MapZone zone = entry.getValue();
            String base = "secrets." + entry.getKey();
            data.set(base + ".world", zone.getWorld());
            data.set(base + ".x", zone.getX());
            data.set(base + ".y", zone.getY());
            data.set(base + ".z", zone.getZ());
            data.set(base + ".radius", zone.getRadius());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("map-secrets.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
