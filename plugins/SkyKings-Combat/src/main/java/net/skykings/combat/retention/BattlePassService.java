package net.skykings.combat.retention;

import net.skykings.core.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Season Battle Pass mit Free/Premium Track, persistentem Claim-Status und ohne Paid-Rank-Rewards. */
public final class BattlePassService implements Listener {
    private static final String TITLE = ChatColor.DARK_PURPLE + "SkyKings | Battle Pass";
    private static final int[] LEVELS = {5, 10, 20, 30, 40, 50, 75, 100};
    private static final int[] FREE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17};
    private static final int[] PREMIUM_SLOTS = {37, 38, 39, 40, 41, 42, 43, 44};

    private final JavaPlugin plugin;
    private final SeasonProgressService progress;
    private final EconomyService economy;
    private final File file;
    private final YamlConfiguration data;

    public BattlePassService(JavaPlugin plugin, SeasonProgressService progress, EconomyService economy) {
        this.plugin = plugin;
        this.progress = progress;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "battlepass.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        inv.setItem(4, item(Material.BOOK, ChatColor.AQUA + "Season " + progress.getSeason(),
                ChatColor.GRAY + "PvP-Level: " + ChatColor.WHITE + progress.getLevel(player.getUniqueId()) + "/100",
                ChatColor.GRAY + "XP: " + ChatColor.WHITE + progress.getXp(player.getUniqueId()),
                isPremium(player.getUniqueId()) ? ChatColor.GOLD + "Premium aktiv" : ChatColor.GRAY + "Free Track"));
        for (int i = 0; i < LEVELS.length; i++) {
            int level = LEVELS[i];
            inv.setItem(FREE_SLOTS[i], rewardItem(player, false, level));
            inv.setItem(PREMIUM_SLOTS[i], rewardItem(player, true, level));
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() == null || !TITLE.equals(event.getInventory().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        for (int i = 0; i < LEVELS.length; i++) {
            if (slot == FREE_SLOTS[i]) { claim(player, false, LEVELS[i]); open(player); return; }
            if (slot == PREMIUM_SLOTS[i]) { claim(player, true, LEVELS[i]); open(player); return; }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory() != null && TITLE.equals(event.getInventory().getTitle())) event.setCancelled(true);
    }

    public boolean setPremium(UUID uuid, boolean enabled) {
        data.set("players." + uuid + ".premium", enabled);
        save();
        return enabled;
    }

    public boolean isPremium(UUID uuid) { return data.getBoolean("players." + uuid + ".premium", false); }

    private void claim(Player player, boolean premium, int level) {
        UUID uuid = player.getUniqueId();
        if (progress.getLevel(uuid) < level) {
            player.sendMessage(ChatColor.RED + "Dafuer brauchst du PvP-Level " + level + ".");
            return;
        }
        if (premium && !isPremium(uuid)) {
            player.sendMessage(ChatColor.GOLD + "Dieser Reward gehoert zum Premium Track.");
            return;
        }
        String path = "players." + uuid + ".claimed." + (premium ? "premium" : "free") + "." + level;
        if (data.getBoolean(path, false)) {
            player.sendMessage(ChatColor.YELLOW + "Diesen Battle-Pass-Reward hast du bereits abgeholt.");
            return;
        }
        long coins = premium ? level * 12_500L : level * 10_000L;
        int stars = Math.max(1, level / (premium ? 10 : 20));
        economy.deposit(uuid, coins, "BATTLE_PASS", (premium ? "Premium" : "Free") + " Level " + level);
        java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, stars));
        for (ItemStack stack : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), stack);
        data.set(path, true);
        save();
        player.sendMessage(ChatColor.GREEN + "Battle Pass Level " + level + " abgeholt: +" + coins + " Coins, +" + stars + " Nethersterne.");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7F, premium ? 1.6F : 1.3F);
    }

    private ItemStack rewardItem(Player player, boolean premium, int level) {
        UUID uuid = player.getUniqueId();
        boolean unlocked = progress.getLevel(uuid) >= level;
        boolean claimed = data.getBoolean("players." + uuid + ".claimed." + (premium ? "premium" : "free") + "." + level, false);
        Material material = claimed ? Material.STAINED_GLASS_PANE : (premium ? Material.GOLD_INGOT : Material.IRON_INGOT);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + (premium ? "Premium Track" : "Free Track"));
        lore.add(ChatColor.GRAY + "Coins: " + ChatColor.WHITE + (premium ? level * 12_500L : level * 10_000L));
        lore.add(ChatColor.GRAY + "Nethersterne: " + ChatColor.WHITE + Math.max(1, level / (premium ? 10 : 20)));
        lore.add(claimed ? ChatColor.GREEN + "Abgeholt" : (unlocked ? ChatColor.YELLOW + "Klicken zum Abholen" : ChatColor.RED + "Noch gesperrt"));
        if (premium && !isPremium(uuid)) lore.add(ChatColor.DARK_GRAY + "Premium nicht aktiv");
        return item(material, (premium ? ChatColor.GOLD : ChatColor.WHITE) + "Level " + level, lore.toArray(new String[0]));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta(); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); stack.setItemMeta(meta); return stack;
    }

    public void save() {
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("battlepass.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }
}
