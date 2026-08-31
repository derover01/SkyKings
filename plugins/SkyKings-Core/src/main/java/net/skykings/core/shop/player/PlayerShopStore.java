package net.skykings.core.shop.player;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** YAML-Persistenz fuer spielereigene Shops. */
public final class PlayerShopStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerShop> shops = new LinkedHashMap<UUID, PlayerShop>();

    public PlayerShopStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-shops.yml");
        load();
    }

    public synchronized Collection<PlayerShop> all() {
        return Collections.unmodifiableCollection(shops.values());
    }

    public synchronized PlayerShop get(UUID id) { return shops.get(id); }

    public synchronized PlayerShop getByVillager(UUID villager) {
        for (PlayerShop shop : shops.values()) {
            if (villager.equals(shop.getVillagerUuid())) return shop;
        }
        return null;
    }

    public synchronized PlayerShop create(UUID owner) {
        PlayerShop shop = new PlayerShop(UUID.randomUUID(), owner);
        shops.put(shop.getId(), shop);
        save();
        return shop;
    }

    public synchronized void delete(UUID id) {
        shops.remove(id);
        save();
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerShop shop : shops.values()) {
            String p = "shops." + shop.getId() + ".";
            yaml.set(p + "owner", shop.getOwner().toString());
            yaml.set(p + "villager", shop.getVillagerUuid() == null ? null : shop.getVillagerUuid().toString());
            yaml.set(p + "material", shop.getMaterial() == null ? null : shop.getMaterial().name());
            yaml.set(p + "data", shop.getData());
            yaml.set(p + "amount", shop.getAmountPerSale());
            yaml.set(p + "price-coins", shop.getPriceCoins());
            yaml.set(p + "stock", shop.getStock());
            yaml.set(p + "pending-revenue", shop.getPendingRevenue());
            yaml.set(p + "world", shop.getWorld());
            yaml.set(p + "x", shop.getX());
            yaml.set(p + "y", shop.getY());
            yaml.set(p + "z", shop.getZ());
        }
        try { yaml.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("PlayerShops konnten nicht gespeichert werden: " + ex.getMessage()); }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("shops");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String base = "shops." + key + ".";
                UUID owner = UUID.fromString(yaml.getString(base + "owner"));
                PlayerShop shop = new PlayerShop(id, owner);
                String villager = yaml.getString(base + "villager");
                if (villager != null && !villager.isEmpty()) shop.setVillagerUuid(UUID.fromString(villager));
                String material = yaml.getString(base + "material");
                if (material != null) shop.setMaterial(Material.matchMaterial(material));
                shop.setData((short) yaml.getInt(base + "data", 0));
                shop.setAmountPerSale(yaml.getInt(base + "amount", 1));
                shop.setPriceCoins(yaml.getLong(base + "price-coins", 0L));
                shop.setStock(yaml.getInt(base + "stock", 0));
                shop.setPendingRevenue(yaml.getLong(base + "pending-revenue", 0L));
                shop.setWorld(yaml.getString(base + "world"));
                shop.setX(yaml.getDouble(base + "x"));
                shop.setY(yaml.getDouble(base + "y"));
                shop.setZ(yaml.getDouble(base + "z"));
                shops.put(id, shop);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Ungueltiger PlayerShop-Eintrag " + key + ": " + ex.getMessage());
            }
        }
    }
}
