package net.skykings.core.shop.player;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

    public PlayerShopStore(JavaPlugin plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "player-shops.yml"); load(); }
    public synchronized Collection<PlayerShop> all() { return Collections.unmodifiableCollection(shops.values()); }
    public synchronized PlayerShop get(UUID id) { return shops.get(id); }
    public synchronized PlayerShop getByVillager(UUID villager) { for (PlayerShop shop : shops.values()) if (villager.equals(shop.getVillagerUuid())) return shop; return null; }
    public synchronized PlayerShop create(UUID owner) { PlayerShop shop = new PlayerShop(UUID.randomUUID(), owner); shops.put(shop.getId(), shop); if (!saveChecked()) { shops.remove(shop.getId()); return null; } return shop; }
    public synchronized void delete(UUID id) { deleteChecked(id); }
    public synchronized boolean deleteChecked(UUID id) { PlayerShop removed = shops.remove(id); if (removed == null) return false; if (saveChecked()) return true; shops.put(id, removed); return false; }
    public synchronized void save() { saveChecked(); }

    public synchronized boolean saveChecked() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerShop shop : shops.values()) {
            String p = "shops." + shop.getId() + ".";
            yaml.set(p + "owner", shop.getOwner().toString());
            yaml.set(p + "villager", shop.getVillagerUuid() == null ? null : shop.getVillagerUuid().toString());
            yaml.set(p + "pending-revenue", shop.getPendingRevenue());
            yaml.set(p + "world", shop.getWorld()); yaml.set(p + "x", shop.getX()); yaml.set(p + "y", shop.getY()); yaml.set(p + "z", shop.getZ());
            for (int i = 0; i < PlayerShop.MAX_OFFERS; i++) {
                PlayerShopOffer offer = shop.getOffer(i);
                String o = p + "offers." + i + ".";
                yaml.set(o + "material", offer.getMaterial() == null ? null : offer.getMaterial().name());
                yaml.set(o + "data", offer.getData());
                yaml.set(o + "top", offer.getAmountTop());
                yaml.set(o + "middle", offer.getAmountMiddle());
                yaml.set(o + "price-coins", offer.getPriceCoins());
            }
        }
        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
            yaml.save(temp);
            try { Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning("PlayerShops konnten nicht sicher gespeichert werden: " + ex.getMessage());
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("shops");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key); String base = "shops." + key + "."; UUID owner = UUID.fromString(yaml.getString(base + "owner"));
                PlayerShop shop = new PlayerShop(id, owner);
                String villager = yaml.getString(base + "villager"); if (villager != null && !villager.isEmpty()) shop.setVillagerUuid(UUID.fromString(villager));
                boolean hasOffers = yaml.isConfigurationSection(base + "offers");
                if (hasOffers) {
                    for (int i = 0; i < PlayerShop.MAX_OFFERS; i++) {
                        String o = base + "offers." + i + "."; PlayerShopOffer offer = shop.getOffer(i);
                        String material = yaml.getString(o + "material"); if (material != null) offer.setMaterial(Material.matchMaterial(material));
                        offer.setData((short) yaml.getInt(o + "data", 0)); offer.setAmountTop(yaml.getInt(o + "top", 0)); offer.setAmountMiddle(yaml.getInt(o + "middle", 0)); offer.setPriceCoins(yaml.getLong(o + "price-coins", 0L));
                    }
                } else {
                    String material = yaml.getString(base + "material");
                    if (material != null) shop.setMaterial(Material.matchMaterial(material));
                    shop.setData((short) yaml.getInt(base + "data", 0)); shop.setAmountPerSale(yaml.getInt(base + "amount", 1)); shop.setPriceCoins(yaml.getLong(base + "price-coins", 0L)); shop.setStock(yaml.getInt(base + "stock", 0));
                }
                shop.setPendingRevenue(yaml.getLong(base + "pending-revenue", 0L)); shop.setWorld(yaml.getString(base + "world")); shop.setX(yaml.getDouble(base + "x")); shop.setY(yaml.getDouble(base + "y")); shop.setZ(yaml.getDouble(base + "z")); shops.put(id, shop);
            } catch (RuntimeException ex) { plugin.getLogger().warning("Ungueltiger PlayerShop-Eintrag " + key + ": " + ex.getMessage()); }
        }
    }
}
