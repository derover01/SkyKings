package net.skykings.combat.retention;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.item.SkyKingsCurrencyItems;
import net.skykings.core.ui.UiItems;
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

/** Visueller Season Battle Pass mit klar getrenntem Free-/Premium-Track. */
public final class BattlePassService implements Listener {
    private static final String TITLE = ChatColor.DARK_PURPLE + "SkyKings " + ChatColor.DARK_GRAY + "| " + ChatColor.GOLD + "Battle Pass";
    private static final int[] LEVELS = {5, 10, 20, 30, 40, 50, 75, 100};
    private static final int[] FREE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17};
    private static final int[] PREMIUM_SLOTS = {37, 38, 39, 40, 41, 42, 43, 44};
    private static volatile BattlePassService active;

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
        active = this;
    }

    public static BattlePassService active() { return active; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        decorate(inv);
        UUID uuid = player.getUniqueId();
        int level = progress.getLevel(uuid);
        inv.setItem(4, item(Material.EXP_BOTTLE, ChatColor.AQUA.toString() + ChatColor.BOLD + "SEASON " + progress.getSeason(),
                ChatColor.GRAY + "PvP-Level: " + ChatColor.WHITE + level + "/100",
                ChatColor.GRAY + "Season-XP: " + ChatColor.WHITE + progress.getXp(uuid),
                ChatColor.GRAY + "Bis zum naechsten Level: " + ChatColor.WHITE + progress.xpToNext(uuid) + " XP",
                "", progressBar(level, 100), "",
                isPremium(uuid) ? ChatColor.GOLD.toString() + ChatColor.BOLD + "PREMIUM AKTIV" : ChatColor.GRAY + "Free Pass aktiv"));
        inv.setItem(9, item(Material.IRON_INGOT, ChatColor.WHITE.toString() + ChatColor.BOLD + "FREE TRACK",
                ChatColor.GRAY + "Fuer jeden Spieler verfuegbar."));
        inv.setItem(36, item(Material.GOLD_INGOT, ChatColor.GOLD.toString() + ChatColor.BOLD + "PREMIUM TRACK",
                isPremium(uuid) ? ChatColor.GREEN + "Freigeschaltet" : ChatColor.RED + "Noch nicht freigeschaltet",
                ChatColor.DARK_GRAY + "Zusaetzliche Premium-Quests inklusive."));
        for (int i = 0; i < LEVELS.length; i++) {
            int milestone = LEVELS[i];
            inv.setItem(FREE_SLOTS[i], rewardItem(player, false, milestone));
            inv.setItem(PREMIUM_SLOTS[i], rewardItem(player, true, milestone));
        }
        inv.setItem(31, item(Material.NETHER_STAR, ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS STERNE",
                ChatColor.GRAY + "Battle-Pass-Rewards enthalten",
                ChatColor.GRAY + "die physische Serverwaehrung."));
        inv.setItem(49, UiItems.back());
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5F, 1.25F);
    }

    private void decorate(Inventory inv) {
        ItemStack dark = pane((short) 15, " ");
        ItemStack purple = pane((short) 10, ChatColor.DARK_PURPLE + "Battle Pass");
        for (int i = 0; i < 54; i++) inv.setItem(i, dark);
        for (int i = 10; i <= 17; i++) inv.setItem(i, pane((short) 7, " "));
        for (int i = 37; i <= 44; i++) inv.setItem(i, pane((short) 4, " "));
        inv.setItem(0, purple); inv.setItem(8, purple); inv.setItem(45, purple); inv.setItem(53, purple);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() == null || !TITLE.equals(event.getInventory().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot == 49) { player.closeInventory(); Bukkit.dispatchCommand(player, "profile"); return; }
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
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F); return;
        }
        if (premium && !isPremium(uuid)) {
            player.sendMessage(ChatColor.GOLD + "Dieser Reward gehoert zum Premium Track.");
            player.playSound(player.getLocation(), Sound.CLICK, 0.6F, 0.6F); return;
        }
        String path = "players." + uuid + ".claimed." + (premium ? "premium" : "free") + "." + level;
        if (data.getBoolean(path, false)) { player.sendMessage(ChatColor.YELLOW + "Diesen Reward hast du bereits abgeholt."); return; }
        long coins = premium ? level * 12_500L : level * 10_000L;
        int stars = Math.max(1, level / (premium ? 10 : 20));
        economy.deposit(uuid, coins, "BATTLE_PASS", (premium ? "Premium" : "Free") + " Level " + level);
        SkyKingsCurrencyItems.give(player, stars);
        data.set(path, true); save();
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "BATTLE PASS " + ChatColor.GREEN + "Level " + level
                + " abgeholt! " + ChatColor.GRAY + "+" + coins + " Coins, +" + stars + " SkyKings Sterne");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, premium ? 1.6F : 1.3F);
    }

    private ItemStack rewardItem(Player player, boolean premium, int level) {
        UUID uuid = player.getUniqueId();
        boolean unlocked = progress.getLevel(uuid) >= level;
        boolean claimed = data.getBoolean("players." + uuid + ".claimed." + (premium ? "premium" : "free") + "." + level, false);
        Material material = claimed ? Material.STAINED_GLASS_PANE : (premium ? Material.GOLD_INGOT : Material.IRON_INGOT);
        List<String> lore = new ArrayList<String>();
        lore.add((premium ? ChatColor.GOLD : ChatColor.WHITE) + (premium ? "Premium Track" : "Free Track"));
        lore.add(ChatColor.GRAY + "Milestone: " + ChatColor.WHITE + "Level " + level);
        lore.add("");
        lore.add(ChatColor.GRAY + "Coins: " + ChatColor.YELLOW + (premium ? level * 12_500L : level * 10_000L));
        lore.add(ChatColor.GRAY + "SkyKings Sterne: " + ChatColor.AQUA + Math.max(1, level / (premium ? 10 : 20)));
        lore.add("");
        lore.add(claimed ? ChatColor.GREEN.toString() + ChatColor.BOLD + "ABGEHOLT"
                : (unlocked ? ChatColor.YELLOW + "Klicken zum Abholen" : ChatColor.RED + "Gesperrt bis Level " + level));
        if (premium && !isPremium(uuid)) lore.add(ChatColor.DARK_GRAY + "Premium nicht aktiv");
        return item(material, (premium ? ChatColor.GOLD : ChatColor.WHITE) + "Level " + level, lore.toArray(new String[0]));
    }

    private String progressBar(int current, int target) {
        int filled = Math.min(20, (int) Math.floor(current * 20D / target));
        StringBuilder out = new StringBuilder(ChatColor.AQUA.toString());
        for (int i = 0; i < 20; i++) { if (i == filled) out.append(ChatColor.DARK_GRAY); out.append('|'); }
        return out.toString();
    }

    private ItemStack pane(short data, String name) {
        ItemStack stack = new ItemStack(Material.STAINED_GLASS_PANE, 1, data); ItemMeta meta = stack.getItemMeta(); meta.setDisplayName(name); stack.setItemMeta(meta); return stack;
    }
    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta(); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); stack.setItemMeta(meta); return stack;
    }

    public void save() {
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("battlepass.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }
}
