package net.skykings.core.shop;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Serverweiter Black Market mit 4h-Rotation und persistenter Angebotsauswahl. */
public final class TravelingMerchantGui {
    private static final long ROTATION_MS = 4L * 60L * 60L * 1000L;
    private static final int ACTIVE_OFFERS = 4;

    private final JavaPlugin plugin;
    private final GuiManager guiManager;
    private final ShopTransactionService transactions;
    private final File file;
    private final Map<String, ShopOffer> pool = new LinkedHashMap<String, ShopOffer>();
    private final List<String> active = new ArrayList<String>();
    private long nextRefresh;

    public TravelingMerchantGui(JavaPlugin plugin, GuiManager guiManager, ShopTransactionService transactions) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.transactions = transactions;
        this.file = new File(plugin.getDataFolder(), "black-market.yml");
        buildPool();
        load();
        ensureCurrent(false);
        scheduleNext();
    }

    public void open(Player player) {
        ensureCurrent(true);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Black Market"), 27);
        int[] slots = {10, 12, 14, 16};
        for (int i = 0; i < active.size() && i < slots.length; i++) {
            ShopOffer offer = pool.get(active.get(i));
            if (offer != null) add(gui, slots[i], offer);
        }
        if (active.isEmpty()) {
            gui.setItem(13, UiItems.empty("Keine Angebote", "Die naechste Rotation wird vorbereitet."));
        }

        long seconds = Math.max(0L, (nextRefresh - System.currentTimeMillis() + 999L) / 1000L);
        gui.setItem(22, UiItems.item(Material.WATCH,
                UiTheme.PRIMARY + "Naechste Rotation",
                UiTheme.MUTED + "Neue Angebote in",
                UiTheme.TEXT + UiFormat.durationSeconds(seconds),
                "",
                UiTheme.MUTED + "Der Bestand ist serverweit identisch."));
        gui.setItem(4, UiItems.item(Material.ENDER_CHEST,
                UiTheme.TEXT + "Black Market",
                UiTheme.MUTED + "Seltene, zeitlich rotierende Angebote.",
                UiTheme.TEXT.toString() + ACTIVE_OFFERS + UiTheme.MUTED + " Angebote pro Rotation"));

        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void add(GuiSession gui, int slot, ShopOffer offer) {
        ItemStack icon = offer.getItem();
        long price = offer.getPrice();
        long balance = countStars(gui.getPlayer());
        String name = UiTheme.TEXT + prettyName(offer.getId(), icon);
        List<String> lore = new ArrayList<String>();
        lore.add(UiTheme.MUTED + "Preis " + UiTheme.TEXT + UiFormat.number(price) + " SkyKings Sterne");
        lore.add(UiTheme.MUTED + "Dein Bestand " + UiTheme.TEXT + UiFormat.number(balance));
        if (balance < price) lore.add(UiTheme.DANGER + "Dir fehlen " + UiFormat.number(price - balance) + " Sterne.");
        else lore.add("");
        lore.add(balance >= price ? UiItems.action("Klicken zum Kaufen") : UiTheme.DISABLED + "Nicht genug Sterne");
        org.bukkit.inventory.meta.ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        icon.setItemMeta(meta);
        gui.setItem(slot, icon, (p,e,s) -> {
            ShopPurchaseResult result = transactions.purchase(p, offer, "black_market");
            if (result == ShopPurchaseResult.SUCCESS) {
                p.sendMessage(UiTheme.SUCCESS + "Kauf erfolgreich");
                p.sendMessage(UiTheme.MUTED + UiFormat.number(price) + " SkyKings Sterne wurden verwendet.");
                SoundFeedback.success(p);
                open(p);
            } else {
                p.sendMessage(UiTheme.DANGER + purchaseError(result));
                SoundFeedback.error(p);
            }
        });
    }

    private void buildPool() {
        pool.put("gapples", new ShopOffer("blackmarket_gapples", new ItemStack(Material.GOLDEN_APPLE, 8, (short) 1), ShopCurrency.NETHERSTARS, 18));
        pool.put("pearls", new ShopOffer("blackmarket_pearls", new ItemStack(Material.ENDER_PEARL, 16), ShopCurrency.NETHERSTARS, 10));
        pool.put("diamonds", new ShopOffer("blackmarket_diamond", new ItemStack(Material.DIAMOND, 16), ShopCurrency.NETHERSTARS, 22));
        pool.put("xp", new ShopOffer("blackmarket_xp", new ItemStack(Material.EXP_BOTTLE, 64), ShopCurrency.NETHERSTARS, 12));
        pool.put("obsidian", new ShopOffer("blackmarket_obsidian", new ItemStack(Material.OBSIDIAN, 32), ShopCurrency.NETHERSTARS, 14));
        pool.put("gold", new ShopOffer("blackmarket_gold", new ItemStack(Material.GOLD_INGOT, 32), ShopCurrency.NETHERSTARS, 15));
        pool.put("arrows", new ShopOffer("blackmarket_arrows", new ItemStack(Material.ARROW, 64), ShopCurrency.NETHERSTARS, 8));
        pool.put("webs", new ShopOffer("blackmarket_webs", new ItemStack(Material.WEB, 16), ShopCurrency.NETHERSTARS, 16));

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
        sword.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        pool.put("sword", new ShopOffer("blackmarket_sword", sword, ShopCurrency.NETHERSTARS, 55));

        ItemStack bow = new ItemStack(Material.BOW, 1);
        bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 5);
        bow.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        pool.put("bow", new ShopOffer("blackmarket_bow", bow, ShopCurrency.NETHERSTARS, 48));
    }

    private synchronized void ensureCurrent(boolean notify) {
        if (!active.isEmpty() && System.currentTimeMillis() < nextRefresh) return;
        List<String> ids = new ArrayList<String>(pool.keySet());
        Collections.shuffle(ids, new Random());
        active.clear();
        for (int i = 0; i < Math.min(ACTIVE_OFFERS, ids.size()); i++) active.add(ids.get(i));
        nextRefresh = System.currentTimeMillis() + ROTATION_MS;
        save();
        if (notify && !Bukkit.getOnlinePlayers().isEmpty()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(UiTheme.PRIMARY + "Black Market refreshed");
                SoundFeedback.notify(online);
            }
        }
    }

    private void scheduleNext() {
        long delayMs = Math.max(1000L, nextRefresh - System.currentTimeMillis());
        long ticks = Math.max(20L, delayMs / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (TravelingMerchantGui.this) {
                active.clear();
                nextRefresh = 0L;
            }
            ensureCurrent(true);
            scheduleNext();
        }, ticks);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextRefresh = yaml.getLong("next-refresh", 0L);
        for (String id : yaml.getStringList("active")) if (pool.containsKey(id)) active.add(id);
    }

    private synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-refresh", nextRefresh);
        yaml.set("active", new ArrayList<String>(active));
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("black-market.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private long countStars(Player player) {
        long count = 0L;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == Material.NETHER_STAR) count += stack.getAmount();
        }
        return count;
    }

    private String prettyName(String id, ItemStack item) {
        String key = id == null ? "" : id.toLowerCase();
        if (key.contains("gapple")) return item.getAmount() + "x OP-Gapple";
        if (key.contains("pearl")) return item.getAmount() + "x Enderperle";
        if (key.contains("diamond")) return item.getAmount() + "x Diamant";
        if (key.contains("xp")) return item.getAmount() + "x XP-Flasche";
        if (key.contains("sword")) return "Black Market Schwert";
        if (key.contains("bow")) return "Black Market Bogen";
        String raw = item.getType().name().toLowerCase().replace('_', ' ');
        return item.getAmount() + "x " + raw;
    }

    private String purchaseError(ShopPurchaseResult result) {
        switch (result) {
            case NOT_ENOUGH_NETHERSTARS: return "Nicht genug SkyKings Sterne.";
            case NOT_ENOUGH_MONEY: return "Nicht genug Coins.";
            case INVENTORY_FULL: return "Inventar voll.";
            default: return "Kauf nicht moeglich.";
        }
    }
}
