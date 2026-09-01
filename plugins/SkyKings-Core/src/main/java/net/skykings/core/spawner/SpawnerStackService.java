package net.skykings.core.spawner;

import net.skykings.core.island.IslandAccessService;
import net.skykings.core.plot.PlotAccessService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Spawner-Stacking + leichtgewichtiges Mob-Stacking fuer private Claims.
 *
 * Spawner lassen sich bis 64 stapeln. Spawner-Mobs desselben Typs werden in einem Radius
 * zusammengefasst, damit grosse Farmen nicht hunderte Entities gleichzeitig erzeugen.
 * Pro Kill wird genau ein Mob normal ausgewertet; der Reststack bleibt erhalten.
 */
public final class SpawnerStackService implements Listener, CommandExecutor {
    private static final int MAX_STACK = 64;
    private static final int MAX_MOB_STACK = 250;
    private static final double MOB_MERGE_RADIUS = 6.0D;
    private static final String MOB_STACK_PREFIX = ChatColor.DARK_GRAY + "[Stack] ";

    private final JavaPlugin plugin;
    private final IslandAccessService islands;
    private final PlotAccessService plots;
    private final File file;
    private final Map<String, Integer> stacks = new HashMap<String, Integer>();

    public SpawnerStackService(JavaPlugin plugin, IslandAccessService islands, PlotAccessService plots) {
        this.plugin = plugin; this.islands = islands; this.plots = plots;
        this.file = new File(plugin.getDataFolder(), "spawner-stacks.yml");
        load();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.MOB_SPAWNER || !inClaimWorld(event.getBlock().getLocation())) return;
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) return;
        stacks.putIfAbsent(key(event.getBlock().getLocation()), 1);
        save();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.MOB_SPAWNER || !inClaimWorld(block.getLocation())) return;
        ItemStack hand = event.getPlayer().getItemInHand();
        if (hand == null || hand.getType() != Material.MOB_SPAWNER || !canBuild(event.getPlayer(), block.getLocation())) return;
        String key = key(block.getLocation());
        int count = Math.max(1, stacks.getOrDefault(key, 1));
        if (count >= MAX_STACK) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Dieser Spawner-Stack ist bereits bei " + MAX_STACK + ".");
            return;
        }
        event.setCancelled(true);
        count++;
        stacks.put(key, count);
        int left = hand.getAmount() - 1;
        if (left <= 0) event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
        else { hand.setAmount(left); event.getPlayer().setItemInHand(hand); }
        event.getPlayer().updateInventory();
        save();
        event.getPlayer().sendMessage(ChatColor.GOLD + "Spawner Stack: " + ChatColor.WHITE + count + "/" + MAX_STACK);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ORB_PICKUP, 0.4F, 1.1F);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.MOB_SPAWNER || !inClaimWorld(block.getLocation()) || !canBuild(event.getPlayer(), block.getLocation())) return;
        event.setCancelled(true);
        String key = key(block.getLocation());
        Integer stored = stacks.remove(key);
        int count = Math.max(1, stored == null ? 1 : stored.intValue());
        Location dropLocation = block.getLocation().clone().add(0.5D, 0.5D, 0.5D);
        block.setType(Material.AIR);
        dropSpawnerItems(dropLocation, count);
        save();
        event.getPlayer().sendMessage(ChatColor.YELLOW + "Spawner-Stack abgebaut: " + count + " Spawner.");
    }

    /** Spawner-Mobs werden einen Tick nach dem Spawn mit nahen identischen Mobs zusammengelegt. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) return;
        final LivingEntity spawned = event.getEntity();
        if (!inClaimWorld(spawned.getLocation()) || excludedMob(spawned.getType())) return;
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (spawned.isDead() || !spawned.isValid()) return;
                mergeNearbyMobs(spawned);
            }
        });
    }

    /** Ein Stack-Kill erzeugt normale Drops/XP fuer genau einen Mob und stellt den Rest wieder her. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(EntityDeathEvent event) {
        final LivingEntity dead = event.getEntity();
        if (!inClaimWorld(dead.getLocation())) return;
        final int count = readMobCount(dead);
        if (count <= 1) return;

        final Location location = dead.getLocation().clone();
        final EntityType type = dead.getType();
        final int remaining = count - 1;
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (location.getWorld() == null || excludedMob(type)) return;
                Entity replacement = location.getWorld().spawnEntity(location, type);
                if (!(replacement instanceof LivingEntity)) {
                    replacement.remove();
                    return;
                }
                applyMobCount((LivingEntity) replacement, remaining);
            }
        });
    }

    private void mergeNearbyMobs(LivingEntity target) {
        int total = readMobCount(target);
        List<LivingEntity> merge = new ArrayList<LivingEntity>();
        for (Entity entity : target.getNearbyEntities(MOB_MERGE_RADIUS, MOB_MERGE_RADIUS, MOB_MERGE_RADIUS)) {
            if (!(entity instanceof LivingEntity) || entity == target || entity.isDead()) continue;
            LivingEntity other = (LivingEntity) entity;
            if (other.getType() != target.getType() || !eligibleMobName(other)) continue;
            int otherCount = readMobCount(other);
            if (total + otherCount > MAX_MOB_STACK) continue;
            total += otherCount;
            merge.add(other);
        }
        if (merge.isEmpty()) return;
        for (LivingEntity other : merge) other.remove();
        applyMobCount(target, total);
    }

    private boolean eligibleMobName(LivingEntity entity) {
        String name = entity.getCustomName();
        return name == null || name.isEmpty() || name.startsWith(MOB_STACK_PREFIX);
    }

    private int readMobCount(LivingEntity entity) {
        String name = entity.getCustomName();
        if (name == null || !name.startsWith(MOB_STACK_PREFIX)) return 1;
        int marker = name.lastIndexOf(" x");
        if (marker < 0) return 1;
        String raw = ChatColor.stripColor(name.substring(marker + 2));
        try { return Math.max(1, Math.min(MAX_MOB_STACK, Integer.parseInt(raw))); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private void applyMobCount(LivingEntity entity, int count) {
        if (count <= 1) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
            return;
        }
        entity.setCustomName(MOB_STACK_PREFIX + ChatColor.GOLD + mobName(entity.getType())
                + ChatColor.GRAY + " x" + ChatColor.WHITE + count);
        entity.setCustomNameVisible(true);
    }

    private String mobName(EntityType type) {
        String raw = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder out = new StringBuilder();
        boolean upper = true;
        for (char c : raw.toCharArray()) {
            if (upper && Character.isLetter(c)) { out.append(Character.toUpperCase(c)); upper = false; }
            else out.append(c);
            if (c == ' ') upper = true;
        }
        return out.toString();
    }

    private boolean excludedMob(EntityType type) {
        return type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.ARMOR_STAND;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player p = (Player) sender;
        Block block = p.getTargetBlock((java.util.Set<Material>) null, 6);
        if (block == null || block.getType() != Material.MOB_SPAWNER) { p.sendMessage(ChatColor.RED + "Schau einen Spawner an."); return true; }
        p.sendMessage(ChatColor.GOLD + "Spawner Stack: " + ChatColor.WHITE + Math.max(1, stacks.getOrDefault(key(block.getLocation()), 1)) + "/" + MAX_STACK);
        return true;
    }

    private boolean inClaimWorld(Location l) { return islands.isIslandWorld(l) || plots.isPlotWorld(l); }
    private boolean canBuild(Player p, Location l) { return p.hasPermission("skykings.admin.claim.bypass") || islands.canBuild(p.getUniqueId(), l) || plots.canBuild(p.getUniqueId(), l); }

    private void dropSpawnerItems(Location location, int amount) {
        int left = amount;
        while (left > 0) {
            int part = Math.min(64, left);
            location.getWorld().dropItemNaturally(location, new ItemStack(Material.MOB_SPAWNER, part));
            left -= part;
        }
    }

    private String key(Location l) { return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ(); }
    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        if (y.getConfigurationSection("stacks") == null) return;
        for (String k : y.getConfigurationSection("stacks").getKeys(false)) stacks.put(k, Math.max(1, y.getInt("stacks." + k, 1)));
    }
    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Integer> e : stacks.entrySet()) y.set("stacks." + e.getKey(), e.getValue());
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); y.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("spawner-stacks.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }
}
