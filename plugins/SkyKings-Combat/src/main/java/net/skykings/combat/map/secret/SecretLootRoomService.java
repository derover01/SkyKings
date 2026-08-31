package net.skykings.combat.map.secret;

import net.skykings.combat.map.MapLootTier;
import net.skykings.core.item.SkyKingsCurrencyItems;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Versteckte Map-Caches: ein globaler Claim, danach Cooldown bis zur naechsten Oeffnung. */
public final class SecretLootRoomService implements Listener {
    public static final class Room {
        String id;
        String world;
        int x, y, z;
        MapLootTier tier;
        long cooldownMs;
        long nextReady;
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Room> rooms = new LinkedHashMap<String, Room>();
    private final Random random = new Random();

    public SecretLootRoomService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "secret-loot-rooms.yml");
        load();
    }

    public Map<String, Room> getRooms() { return new LinkedHashMap<String, Room>(rooms); }

    public boolean add(String idRaw, Block chest, MapLootTier tier, long cooldownMinutes) {
        if (chest == null || chest.getType() != Material.CHEST || tier == null) return false;
        String id = normalize(idRaw);
        Room room = new Room();
        room.id = id;
        room.world = chest.getWorld().getName();
        room.x = chest.getX(); room.y = chest.getY(); room.z = chest.getZ();
        room.tier = tier;
        room.cooldownMs = cooldownMinutes > 0 ? cooldownMinutes * 60_000L : tier.getCooldownMillis();
        room.nextReady = 0L;
        rooms.put(id, room);
        save();
        return true;
    }

    public boolean remove(String id) {
        boolean removed = rooms.remove(normalize(id)) != null;
        if (removed) save();
        return removed;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Room room = find(event.getClickedBlock());
        if (room == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (room.nextReady > now) {
            long seconds = (room.nextReady - now + 999L) / 1000L;
            player.sendMessage(UiTheme.MUTED + "Secret Cache " + UiTheme.WARNING + "COOLDOWN " + UiFormat.durationSeconds(seconds));
            SoundFeedback.warning(player);
            return;
        }

        List<ItemStack> loot = room.tier.rollLoot(random);
        int rewards = 0;
        for (ItemStack stack : loot) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            ItemStack reward = stack.getType() == Material.NETHER_STAR
                    ? SkyKingsCurrencyItems.star(stack.getAmount()) : stack;
            Map<Integer, ItemStack> left = player.getInventory().addItem(reward);
            for (ItemStack overflow : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            rewards++;
        }
        room.nextReady = now + room.cooldownMs;
        save();
        player.updateInventory();
        player.sendMessage(UiTheme.PRIMARY + "Secret Cache");
        player.sendMessage(UiTheme.TEXT.toString() + rewards + UiTheme.MUTED + " Loot-Stacks gefunden. "
                + UiTheme.DISABLED + "Respawn in " + UiFormat.durationSeconds(room.cooldownMs / 1000L));
        SoundFeedback.reward(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Room room = find(event.getBlock());
        if (room == null) return;
        if (event.getPlayer().hasPermission("skykings.admin.secretroom")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(UiTheme.DANGER + "Dieser Secret Cache ist geschuetzt.");
        SoundFeedback.error(event.getPlayer());
    }

    public Room find(Block block) {
        if (block == null || block.getWorld() == null) return null;
        for (Room room : rooms.values()) {
            if (!room.world.equals(block.getWorld().getName())) continue;
            if (room.x == block.getX() && room.y == block.getY() && room.z == block.getZ()) return room;
        }
        return null;
    }

    private String normalize(String raw) {
        return raw == null ? "cache" : raw.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("rooms");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            try {
                String base = "rooms." + id + ".";
                Room room = new Room();
                room.id = id;
                room.world = yaml.getString(base + "world");
                room.x = yaml.getInt(base + "x"); room.y = yaml.getInt(base + "y"); room.z = yaml.getInt(base + "z");
                room.tier = MapLootTier.valueOf(yaml.getString(base + "tier", "RARE"));
                room.cooldownMs = Math.max(60_000L, yaml.getLong(base + "cooldown-ms", room.tier.getCooldownMillis()));
                room.nextReady = Math.max(0L, yaml.getLong(base + "next-ready", 0L));
                if (room.world != null) rooms.put(id, room);
            } catch (RuntimeException ignored) { }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Room room : rooms.values()) {
            String base = "rooms." + room.id + ".";
            yaml.set(base + "world", room.world);
            yaml.set(base + "x", room.x); yaml.set(base + "y", room.y); yaml.set(base + "z", room.z);
            yaml.set(base + "tier", room.tier.name());
            yaml.set(base + "cooldown-ms", room.cooldownMs);
            yaml.set(base + "next-ready", room.nextReady);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("secret-loot-rooms.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
