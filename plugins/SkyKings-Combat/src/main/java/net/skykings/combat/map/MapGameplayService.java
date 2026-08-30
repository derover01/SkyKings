package net.skykings.combat.map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Map-Loot + Supply-Drop-Grundsystem. Alle Map-Koordinaten werden ingame registriert,
 * damit die finale Map ohne Codeänderung eingerichtet werden kann.
 */
public final class MapGameplayService implements Listener, CommandExecutor {

    private static final long SUPPLY_LIFETIME_TICKS = 20L * 60L * 10L;

    private static final class LootChestData {
        final MapLootTier tier;
        volatile long nextRefillAt;
        LootChestData(MapLootTier tier, long nextRefillAt) {
            this.tier = tier;
            this.nextRefillAt = nextRefillAt;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, LootChestData> lootChests = new HashMap<String, LootChestData>();
    private final List<String> supplyPoints = new ArrayList<String>();
    private final Random random = new Random();
    private final ExecutorService writer;

    public MapGameplayService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "map-gameplay.yml");
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SkyKings-Map-Gameplay");
            t.setDaemon(true);
            return t;
        });
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tryAutomaticSupplyDrop, 20L * 60L * 45L, 20L * 60L * 45L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfügbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) {
            player.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("maploot")) return handleMapLoot(player, args);
        if (command.getName().equalsIgnoreCase("supplydrop")) return handleSupplyDrop(player, args);
        return true;
    }

    private boolean handleMapLoot(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.YELLOW + "/maploot set <common|rare|epic>" + ChatColor.GRAY + " • Chest ansehen");
            player.sendMessage(ChatColor.YELLOW + "/maploot remove" + ChatColor.GRAY + " • Chest ansehen");
            player.sendMessage(ChatColor.YELLOW + "/maploot refill" + ChatColor.GRAY + " • alle Chests sofort füllen");
            return true;
        }
        if (args[0].equalsIgnoreCase("set") && args.length >= 2) {
            MapLootTier tier = MapLootTier.parse(args[1]);
            if (tier == null || tier == MapLootTier.SUPPLY) {
                player.sendMessage(ChatColor.RED + "Tier: common, rare oder epic.");
                return true;
            }
            Block target = player.getTargetBlock((java.util.Set<Material>) null, 6);
            if (target == null || target.getType() != Material.CHEST) {
                player.sendMessage(ChatColor.RED + "Du musst eine normale Chest ansehen.");
                return true;
            }
            synchronized (lootChests) {
                lootChests.put(key(target.getLocation()), new LootChestData(tier, 0L));
            }
            refill(target.getLocation(), tier, true);
            saveAsync();
            player.sendMessage(tier.getColor() + tier.getDisplay() + ChatColor.GRAY + " Map-Loot-Chest registriert.");
            return true;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            Block target = player.getTargetBlock((java.util.Set<Material>) null, 6);
            if (target == null) return true;
            LootChestData removed;
            synchronized (lootChests) { removed = lootChests.remove(key(target.getLocation())); }
            if (removed != null) {
                saveAsync();
                player.sendMessage(ChatColor.GREEN + "Map-Loot-Chest entfernt.");
            } else player.sendMessage(ChatColor.RED + "Diese Chest ist nicht registriert.");
            return true;
        }
        if (args[0].equalsIgnoreCase("refill")) {
            refillAll();
            player.sendMessage(ChatColor.GREEN + "Alle geladenen Map-Loot-Chests wurden neu gefüllt.");
            return true;
        }
        player.sendMessage(ChatColor.RED + "Ungültiger /maploot-Unterbefehl.");
        return true;
    }

    private boolean handleSupplyDrop(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/supplydrop addpoint" + ChatColor.GRAY + " • aktueller Block");
            player.sendMessage(ChatColor.YELLOW + "/supplydrop trigger" + ChatColor.GRAY + " • zufälliger registrierter Punkt");
            player.sendMessage(ChatColor.YELLOW + "/supplydrop points" + ChatColor.GRAY + " • Anzahl Punkte");
            return true;
        }
        if (args[0].equalsIgnoreCase("addpoint")) {
            Location block = player.getLocation().getBlock().getLocation();
            String key = key(block);
            synchronized (supplyPoints) {
                if (!supplyPoints.contains(key)) supplyPoints.add(key);
            }
            saveAsync();
            player.sendMessage(ChatColor.GREEN + "Supply-Drop-Punkt gespeichert.");
            return true;
        }
        if (args[0].equalsIgnoreCase("points")) {
            synchronized (supplyPoints) {
                player.sendMessage(ChatColor.GOLD + "Supply-Drop-Punkte: " + ChatColor.WHITE + supplyPoints.size());
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("trigger")) {
            if (!spawnSupplyDrop()) player.sendMessage(ChatColor.RED + "Kein gültiger Supply-Drop-Punkt verfügbar.");
            return true;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        LootChestData data;
        synchronized (lootChests) { data = lootChests.get(key(block.getLocation())); }
        if (data == null || block.getType() != Material.CHEST) return;
        if (data.tier == MapLootTier.SUPPLY) return;

        long now = System.currentTimeMillis();
        if (now >= data.nextRefillAt) {
            refill(block.getLocation(), data.tier, false);
            data.nextRefillAt = now + data.tier.getCooldownMillis();
            saveAsync();
        } else {
            long seconds = Math.max(1L, (data.nextRefillAt - now + 999L) / 1000L);
            event.getPlayer().sendMessage(data.tier.getColor() + data.tier.getDisplay() + ChatColor.GRAY
                    + " • nächster Refill in " + ChatColor.WHITE + format(seconds));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isProtected(event.getBlock().getLocation())) return;
        if (event.getPlayer().hasPermission("skykings.admin.map")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Diese Loot-Chest gehört zur SkyPvP-Map.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) { event.blockList().removeIf(block -> isProtected(block.getLocation())); }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) { event.blockList().removeIf(block -> isProtected(block.getLocation())); }

    public void shutdown() {
        saveAsync();
        writer.shutdown();
        try { writer.awaitTermination(3, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        synchronized (lootChests) {
            for (Map.Entry<String, LootChestData> entry : lootChests.entrySet()) {
                LootChestData data = entry.getValue();
                if (data.tier == MapLootTier.SUPPLY) continue;
                if (data.nextRefillAt <= 0L || now < data.nextRefillAt) continue;
                Location location = parseLocation(entry.getKey());
                if (location == null || location.getWorld() == null || !location.getChunk().isLoaded()) continue;
                Block block = location.getBlock();
                if (block.getType() == Material.CHEST) {
                    refill(location, data.tier, false);
                    data.nextRefillAt = now + data.tier.getCooldownMillis();
                }
            }
        }
    }

    private void refillAll() {
        synchronized (lootChests) {
            for (Map.Entry<String, LootChestData> entry : lootChests.entrySet()) {
                if (entry.getValue().tier == MapLootTier.SUPPLY) continue;
                Location location = parseLocation(entry.getKey());
                if (location == null || location.getWorld() == null) continue;
                refill(location, entry.getValue().tier, true);
                entry.getValue().nextRefillAt = System.currentTimeMillis() + entry.getValue().tier.getCooldownMillis();
            }
        }
        saveAsync();
    }

    private void refill(Location location, MapLootTier tier, boolean force) {
        if (location == null || location.getWorld() == null) return;
        Block block = location.getBlock();
        if (block.getType() != Material.CHEST) return;
        Chest chest = (Chest) block.getState();
        Inventory inv = chest.getBlockInventory();
        if (!force && !isEmpty(inv)) return;
        inv.clear();
        List<Integer> slots = new ArrayList<Integer>();
        for (int i = 0; i < inv.getSize(); i++) slots.add(i);
        Collections.shuffle(slots, random);
        List<ItemStack> loot = tier.rollLoot(random);
        for (int i = 0; i < loot.size() && i < slots.size(); i++) inv.setItem(slots.get(i), loot.get(i));
        chest.update(true);
    }

    private boolean spawnSupplyDrop() {
        String raw;
        synchronized (supplyPoints) {
            if (supplyPoints.isEmpty()) return false;
            raw = supplyPoints.get(random.nextInt(supplyPoints.size()));
        }
        final Location location = parseLocation(raw);
        if (location == null || location.getWorld() == null) return false;
        Block block = location.getBlock();
        if (block.getType() != Material.AIR && block.getType() != Material.CHEST) return false;
        block.setType(Material.CHEST);
        refill(location, MapLootTier.SUPPLY, true);
        final String locationKey = key(location);
        synchronized (lootChests) {
            lootChests.put(locationKey, new LootChestData(MapLootTier.SUPPLY, Long.MAX_VALUE));
        }
        saveAsync();
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SUPPLY DROP " + ChatColor.GRAY
                + "• Eine seltene Loot-Chest ist auf der SkyPvP-Map erschienen!");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean remove;
            synchronized (lootChests) {
                LootChestData current = lootChests.get(locationKey);
                remove = current != null && current.tier == MapLootTier.SUPPLY;
                if (remove) lootChests.remove(locationKey);
            }
            if (remove && location.getWorld() != null && location.getBlock().getType() == Material.CHEST) {
                location.getBlock().setType(Material.AIR);
                saveAsync();
                Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "Der Supply Drop ist verschwunden.");
            }
        }, SUPPLY_LIFETIME_TICKS);
        return true;
    }

    private void tryAutomaticSupplyDrop() { if (!supplyPoints.isEmpty()) spawnSupplyDrop(); }

    private boolean isProtected(Location location) {
        synchronized (lootChests) { return lootChests.containsKey(key(location)); }
    }

    private boolean isEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) if (item != null && item.getType() != Material.AIR) return false;
        return true;
    }

    private String key(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    private Location parseLocation(String raw) {
        try {
            String[] parts = raw.split(";");
            if (parts.length != 4) return null;
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (RuntimeException ex) { return null; }
    }

    private String format(long seconds) {
        long m = seconds / 60L;
        long s = seconds % 60L;
        return String.format("%02d:%02d", m, s);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("loot-chests") != null) {
            for (String id : yaml.getConfigurationSection("loot-chests").getKeys(false)) {
                String tierName = yaml.getString("loot-chests." + id + ".tier");
                String location = yaml.getString("loot-chests." + id + ".location");
                long next = yaml.getLong("loot-chests." + id + ".next-refill-at", 0L);
                MapLootTier tier = MapLootTier.parse(tierName);
                if (tier != null && tier != MapLootTier.SUPPLY && location != null) lootChests.put(location, new LootChestData(tier, next));
            }
        }
        supplyPoints.addAll(yaml.getStringList("supply-points"));
    }

    private void saveAsync() {
        final Map<String, LootChestData> chestSnapshot = new HashMap<String, LootChestData>();
        synchronized (lootChests) { chestSnapshot.putAll(lootChests); }
        final List<String> pointSnapshot;
        synchronized (supplyPoints) { pointSnapshot = new ArrayList<String>(supplyPoints); }
        writer.submit(() -> save(chestSnapshot, pointSnapshot));
    }

    private void save(Map<String, LootChestData> chests, List<String> points) {
        YamlConfiguration yaml = new YamlConfiguration();
        int index = 0;
        for (Map.Entry<String, LootChestData> entry : chests.entrySet()) {
            if (entry.getValue().tier == MapLootTier.SUPPLY) continue;
            String path = "loot-chests.c" + index++;
            yaml.set(path + ".location", entry.getKey());
            yaml.set(path + ".tier", entry.getValue().tier.name());
            yaml.set(path + ".next-refill-at", entry.getValue().nextRefillAt);
        }
        yaml.set("supply-points", points);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Map-Gameplay konnte nicht gespeichert werden.", ex);
        }
    }
}
