package net.skykings.core.spawner;

import net.skykings.core.island.IslandAccessService;
import net.skykings.core.plot.PlotAccessService;
import net.skykings.core.transaction.GameplaySettlementJournal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Spawner-Stacking + leichtgewichtiges Mob-Stacking fuer private Claims.
 *
 * Spawner lassen sich bis 64 stapeln. Benutzerdefinierte Stack-Transaktionen werden ueber das
 * Core-Gameplay-Journal abgesichert, weil Stack-YAML, Spieler-Inventar und Weltzustand getrennte
 * Persistenzsysteme sind. Ein unklarer Crash-Zustand wird nie automatisch erneut ausgefuehrt.
 */
public final class SpawnerStackService implements Listener, CommandExecutor {
    private static final int MAX_STACK = 64;
    private static final int MAX_MOB_STACK = 250;
    private static final double MOB_MERGE_RADIUS = 6.0D;
    private static final String MOB_STACK_PREFIX = ChatColor.DARK_GRAY + "[Stack] ";

    private final JavaPlugin plugin;
    private final IslandAccessService islands;
    private final PlotAccessService plots;
    private final GameplaySettlementJournal settlementJournal;
    private final File file;
    private final Map<String, Integer> stacks = new HashMap<String, Integer>();

    public SpawnerStackService(JavaPlugin plugin, IslandAccessService islands, PlotAccessService plots) {
        this.plugin = plugin;
        this.islands = islands;
        this.plots = plots;
        this.settlementJournal = resolveJournal(plugin);
        this.file = new File(plugin.getDataFolder(), "spawner-stacks.yml");
        load();
    }

    private GameplaySettlementJournal resolveJournal(JavaPlugin plugin) {
        GameplaySettlementJournal existing = GameplaySettlementJournal.active();
        return existing != null ? existing : new GameplaySettlementJournal(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.MOB_SPAWNER || !inClaimWorld(event.getBlock().getLocation())) return;
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) return;
        stacks.putIfAbsent(key(event.getBlock().getLocation()), 1);
        save();
    }

    @EventHandler(ignoreCancelled = true)
    public synchronized void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.MOB_SPAWNER || !inClaimWorld(block.getLocation())) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() != Material.MOB_SPAWNER || !canBuild(player, block.getLocation())) return;
        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        if (settlementJournal.hasPendingFor(playerId)) {
            reviewMessage(player);
            return;
        }

        String blockKey = key(block.getLocation());
        int previousCount = Math.max(1, stacks.getOrDefault(blockKey, 1));
        if (previousCount >= MAX_STACK) {
            player.sendMessage(ChatColor.RED + "Dieser Spawner-Stack ist bereits bei " + MAX_STACK + ".");
            return;
        }

        UUID transaction = settlementJournal.begin(playerId, "SPAWNER_STACK_ADD", blockKey,
                "before=" + previousCount + ", after=" + (previousCount + 1));
        if (transaction == null) {
            player.sendMessage(ChatColor.RED + "Der Spawner konnte nicht sicher gestackt werden.");
            return;
        }

        ItemStack handBefore = hand.clone();
        int newCount = previousCount + 1;
        stacks.put(blockKey, newCount);
        int left = hand.getAmount() - 1;
        if (left <= 0) player.setItemInHand(new ItemStack(Material.AIR));
        else {
            hand.setAmount(left);
            player.setItemInHand(hand);
        }
        player.updateInventory();

        if (!saveNow()) {
            stacks.put(blockKey, previousCount);
            player.setItemInHand(handBefore);
            player.updateInventory();
            if (!savePlayerData(player)) {
                settlementJournal.noteFailure(transaction, "STACK_YAML_FAILED_AND_INVENTORY_ROLLBACK_COMMIT_FAILED");
                reviewMessage(player);
                return;
            }
            closeOrReview(transaction, player, "STACK_YAML_FAILED_ROLLBACK_JOURNAL_CLOSE_FAILED");
            player.sendMessage(ChatColor.RED + "Der Spawner-Stack konnte nicht gespeichert werden.");
            return;
        }

        if (!savePlayerData(player)) {
            settlementJournal.noteFailure(transaction, "STACK_YAML_COMMITTED_BUT_PLAYERDATA_COMMIT_FAILED");
            reviewMessage(player);
            return;
        }
        if (!closeOrReview(transaction, player, "STACK_ADD_COMMITTED_BUT_JOURNAL_CLOSE_FAILED")) return;

        player.sendMessage(ChatColor.GOLD + "Spawner Stack: " + ChatColor.WHITE + newCount + "/" + MAX_STACK);
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.4F, 1.1F);
    }

    @EventHandler(ignoreCancelled = true)
    public synchronized void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.MOB_SPAWNER || !inClaimWorld(block.getLocation()) || !canBuild(event.getPlayer(), block.getLocation())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (settlementJournal.hasPendingFor(playerId)) {
            reviewMessage(player);
            return;
        }

        String blockKey = key(block.getLocation());
        Integer stored = stacks.get(blockKey);
        int count = Math.max(1, stored == null ? 1 : stored.intValue());
        ItemStack reward = new ItemStack(Material.MOB_SPAWNER, count);
        if (!canFit(player, reward)) {
            player.sendMessage(ChatColor.RED + "Du brauchst genug Inventarplatz fuer " + count + " Spawner.");
            return;
        }

        UUID transaction = settlementJournal.begin(playerId, "SPAWNER_STACK_BREAK", blockKey,
                "count=" + count + ", world=" + block.getWorld().getName());
        if (transaction == null) {
            player.sendMessage(ChatColor.RED + "Der Spawner-Stack konnte nicht sicher abgebaut werden.");
            return;
        }

        stacks.remove(blockKey);
        if (!saveNow()) {
            if (stored == null) stacks.remove(blockKey); else stacks.put(blockKey, stored);
            closeOrReview(transaction, player, "STACK_REMOVE_NOT_COMMITTED_JOURNAL_CLOSE_FAILED");
            player.sendMessage(ChatColor.RED + "Der Spawner-Stack konnte nicht gespeichert werden.");
            return;
        }

        block.setType(Material.AIR);
        if (!saveWorld(block.getWorld())) {
            settlementJournal.noteFailure(transaction, "STACK_YAML_COMMITTED_BUT_WORLD_COMMIT_FAILED");
            reviewMessage(player);
            return;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(reward);
        if (leftovers != null && !leftovers.isEmpty()) {
            settlementJournal.noteFailure(transaction, "WORLD_COMMITTED_BUT_REWARD_DELIVERY_PARTIAL");
            reviewMessage(player);
            return;
        }
        player.updateInventory();
        if (!savePlayerData(player)) {
            settlementJournal.noteFailure(transaction, "WORLD_AND_STACK_COMMITTED_BUT_PLAYERDATA_COMMIT_FAILED");
            reviewMessage(player);
            return;
        }
        if (!closeOrReview(transaction, player, "STACK_BREAK_COMMITTED_BUT_JOURNAL_CLOSE_FAILED")) return;

        player.sendMessage(ChatColor.YELLOW + "Spawner-Stack abgebaut: " + count + " Spawner.");
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
            player.updateInventory();
            player.saveData();
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Spielerdaten konnten nach Spawner-Transaktion nicht gespeichert werden: " + player.getUniqueId(), ex);
            return false;
        }
    }

    private boolean saveWorld(World world) {
        try {
            world.save();
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Welt konnte nach Spawner-Transaktion nicht synchron gespeichert werden: " + world.getName(), ex);
            return false;
        }
    }

    private boolean closeOrReview(UUID transaction, Player player, String failureReason) {
        if (settlementJournal.complete(transaction)) return true;
        settlementJournal.noteFailure(transaction, failureReason);
        reviewMessage(player);
        return false;
    }

    private void reviewMessage(Player player) {
        player.sendMessage(ChatColor.RED + "Eine vorherige Gameplay-Transaktion muss zuerst von Staff geprueft werden.");
        player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.5F, 0.7F);
    }

    private String key(Location l) { return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ(); }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        if (y.getConfigurationSection("stacks") == null) return;
        for (String k : y.getConfigurationSection("stacks").getKeys(false)) stacks.put(k, Math.max(1, y.getInt("stacks." + k, 1)));
    }

    private boolean saveNow() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Integer> e : stacks.entrySet()) y.set("stacks." + e.getKey(), e.getValue());
        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
            y.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "spawner-stacks.yml konnte nicht atomar gespeichert werden.", ex);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }

    public void save() {
        saveNow();
    }
}
