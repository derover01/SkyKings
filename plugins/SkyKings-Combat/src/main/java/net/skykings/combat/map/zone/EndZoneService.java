package net.skykings.combat.map.zone;

import net.skykings.core.economy.EconomyService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistente End-Zone mit eigenen Kill-Rewards und Anti-Farm-Cooldown. */
public final class EndZoneService implements Listener {
    private static final long COIN_REWARD = 50_000L;
    private static final int STAR_REWARD = 2;
    private static final long SAME_VICTIM_COOLDOWN = 10L * 60L * 1000L;

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final MapMasteryService mastery;
    private final File file;
    private final Map<String, Long> pairCooldowns = new HashMap<String, Long>();
    private MapZone zone;

    public EndZoneService(JavaPlugin plugin, EconomyService economy, MapMasteryService mastery) {
        this.plugin = plugin;
        this.economy = economy;
        this.mastery = mastery;
        this.file = new File(plugin.getDataFolder(), "end-zone.yml");
        load();
    }

    public MapZone getZone() { return zone; }

    public void set(Player player, double radius) {
        zone = new MapZone("end-zone", player.getWorld().getName(), player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(), radius);
        save();
    }

    public void remove() {
        zone = null;
        save();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (zone == null) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) return;
        if (!zone.contains(victim.getLocation()) || !zone.contains(killer.getLocation())) return;

        String key = killer.getUniqueId().toString() + ":" + victim.getUniqueId().toString();
        long now = System.currentTimeMillis();
        Long next = pairCooldowns.get(key);
        if (next != null && now < next.longValue()) return;
        pairCooldowns.put(key, now + SAME_VICTIM_COOLDOWN);

        economy.deposit(killer.getUniqueId(), COIN_REWARD, "END_ZONE", "End Zone Kill");
        java.util.Map<Integer, ItemStack> left = killer.getInventory().addItem(new ItemStack(Material.NETHER_STAR, STAR_REWARD));
        for (ItemStack stack : left.values()) killer.getWorld().dropItemNaturally(killer.getLocation(), stack);
        mastery.addEndKill(killer.getUniqueId());
        killer.updateInventory();
        killer.sendMessage(ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD + "END ZONE " + ChatColor.LIGHT_PURPLE
                + "+" + COIN_REWARD + " Coins, +" + STAR_REWARD + " Nethersterne");
        killer.playSound(killer.getLocation(), Sound.ENDERMAN_TELEPORT, 0.7F, 1.35F);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String world = yaml.getString("zone.world");
        if (world == null || world.trim().isEmpty()) return;
        zone = new MapZone("end-zone", world, yaml.getDouble("zone.x"), yaml.getDouble("zone.y"),
                yaml.getDouble("zone.z"), yaml.getDouble("zone.radius", 12D));
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
            plugin.getLogger().warning("end-zone.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
