package net.skykings.core.shop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Zentrale, konfigurierbare Buy-/Sell-Preisliste für die SkyKings-Economy. */
public final class ShopPriceRegistry {

    public static final class Price {
        private final long buy;
        private final long sell;

        Price(long buy, long sell) {
            this.buy = Math.max(0L, buy);
            this.sell = Math.max(0L, sell);
        }

        public long getBuy() { return buy; }
        public long getSell() { return sell; }
    }

    private final Map<String, Price> prices = new LinkedHashMap<String, Price>();

    public ShopPriceRegistry(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "shops.yml");
        if (!file.exists()) plugin.saveResource("shops.yml", false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("items");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            Material material = Material.matchMaterial(section.getString("material", key));
            if (material == null) {
                plugin.getLogger().warning("Unbekanntes Shop-Material: " + key);
                continue;
            }
            short data = (short) section.getInt("data", 0);
            prices.put(key(material, data), new Price(section.getLong("buy", 0L), section.getLong("sell", 0L)));
        }
        plugin.getLogger().info("Shop-Preise registriert: " + prices.size());
    }

    public Price get(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        return prices.get(key(item.getType(), item.getDurability()));
    }

    public long getSellValue(ItemStack item) {
        if (!isSellable(item)) return 0L;
        Price price = get(item);
        if (price == null || price.getSell() <= 0L) return 0L;
        try {
            return Math.multiplyExact(price.getSell(), item.getAmount());
        } catch (ArithmeticException ex) {
            return Long.MAX_VALUE;
        }
    }

    public boolean isSellable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.NETHER_STAR) return false;
        if (item.hasItemMeta()) {
            if (item.getItemMeta().hasDisplayName() || item.getItemMeta().hasLore() || item.getItemMeta().hasEnchants()) return false;
        }
        Price price = get(item);
        return price != null && price.getSell() > 0L;
    }

    private String key(Material material, short data) {
        return material.name().toUpperCase(Locale.ROOT) + ":" + data;
    }
}
