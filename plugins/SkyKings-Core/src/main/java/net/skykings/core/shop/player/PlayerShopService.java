package net.skykings.core.shop.player;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.logging.LoggingService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Sichere Transaktionen fuer spielereigene Shops.
 * Einnahmen landen zuerst als persistentes Guthaben im Shop und koennen vom Besitzer abgeholt werden.
 */
public final class PlayerShopService {
    public enum Result {
        SUCCESS, NOT_ALLOWED, INVALID_SHOP, OUT_OF_STOCK, NOT_ENOUGH_MONEY, INVENTORY_FULL, FAILED
    }

    private static final int FEE_PERCENT = 5;

    private final PlayerShopStore store;
    private final EconomyService economy;
    private final LoggingService logging;
    private ShopPlacementPolicy placementPolicy;

    public PlayerShopService(PlayerShopStore store, EconomyService economy, LoggingService logging) {
        this.store = store;
        this.economy = economy;
        this.logging = logging;
        this.placementPolicy = new ShopPlacementPolicy() {
            @Override public boolean canCreateShop(Player player, Location location) { return false; }
            @Override public boolean canManageShop(Player player, PlayerShop shop) {
                return player != null && shop != null && player.getUniqueId().equals(shop.getOwner());
            }
        };
    }

    public void setPlacementPolicy(ShopPlacementPolicy placementPolicy) {
        if (placementPolicy != null) this.placementPolicy = placementPolicy;
    }

    public PlayerShop create(Player owner, Location location) {
        if (owner == null || location == null || !placementPolicy.canCreateShop(owner, location)) return null;
        PlayerShop shop = store.create(owner.getUniqueId());
        shop.setWorld(location.getWorld() == null ? null : location.getWorld().getName());
        shop.setX(location.getX()); shop.setY(location.getY()); shop.setZ(location.getZ());
        store.save();
        return shop;
    }

    public Result purchase(Player buyer, UUID shopId) {
        PlayerShop shop = store.get(shopId);
        if (buyer == null || shop == null || !shop.isConfigured()) return Result.INVALID_SHOP;
        if (shop.getStock() < shop.getAmountPerSale()) return Result.OUT_OF_STOCK;

        ItemStack reward = new ItemStack(shop.getMaterial(), shop.getAmountPerSale(), shop.getData());
        if (!canFit(buyer, reward)) return Result.INVENTORY_FULL;
        long price = shop.getPriceCoins();
        if (!economy.has(buyer.getUniqueId(), price)) return Result.NOT_ENOUGH_MONEY;
        if (!economy.withdraw(buyer.getUniqueId(), price, "PLAYER_SHOP", "Kauf Shop " + shopId)) return Result.NOT_ENOUGH_MONEY;

        if (!buyer.getInventory().addItem(reward).isEmpty()) {
            economy.deposit(buyer.getUniqueId(), price, "PLAYER_SHOP_ROLLBACK", "Rollback Shop " + shopId);
            return Result.FAILED;
        }

        long fee = Math.max(1L, price * FEE_PERCENT / 100L);
        long sellerRevenue = Math.max(0L, price - fee);
        shop.setStock(shop.getStock() - shop.getAmountPerSale());
        shop.setPendingRevenue(shop.getPendingRevenue() + sellerRevenue);
        store.save();

        logging.log(new AuditEvent(AuditEventType.SHOP_PURCHASE, buyer.getUniqueId(), "PLAYER_SHOP", price,
                "shop=" + shopId + ", owner=" + shop.getOwner() + ", fee=" + fee + ", item=" + shop.getMaterial()
                        + ", amount=" + shop.getAmountPerSale()));
        buyer.updateInventory();
        return Result.SUCCESS;
    }

    public long claimRevenue(Player owner, UUID shopId) {
        PlayerShop shop = store.get(shopId);
        if (owner == null || shop == null || !placementPolicy.canManageShop(owner, shop)) return 0L;
        long amount = shop.getPendingRevenue();
        if (amount <= 0L) return 0L;
        shop.setPendingRevenue(0L);
        store.save();
        economy.deposit(owner.getUniqueId(), amount, "PLAYER_SHOP", "Shop-Einnahmen " + shopId);
        return amount;
    }

    public boolean addStock(Player owner, UUID shopId, int amount) {
        PlayerShop shop = store.get(shopId);
        if (owner == null || shop == null || amount <= 0 || !placementPolicy.canManageShop(owner, shop)) return false;
        ItemStack hand = owner.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR || hand.getAmount() < amount) return false;
        if (shop.getMaterial() == null) {
            shop.setMaterial(hand.getType());
            shop.setData(hand.getDurability());
        }
        if (shop.getMaterial() != hand.getType() || shop.getData() != hand.getDurability()) return false;
        if (hand.hasItemMeta() && hand.getItemMeta() != null && (hand.getItemMeta().hasDisplayName() || hand.getItemMeta().hasLore())) return false;
        if (!hand.getEnchantments().isEmpty()) return false;

        int remaining = hand.getAmount() - amount;
        if (remaining <= 0) owner.setItemInHand(new ItemStack(Material.AIR));
        else { hand.setAmount(remaining); owner.setItemInHand(hand); }
        shop.setStock(shop.getStock() + amount);
        store.save();
        owner.updateInventory();
        return true;
    }

    /** Gibt normalen Stock sicher an den Besitzer zurueck. */
    public boolean withdrawStock(Player owner, UUID shopId, int amount) {
        PlayerShop shop = store.get(shopId);
        if (owner == null || shop == null || amount <= 0 || !placementPolicy.canManageShop(owner, shop)) return false;
        if (shop.getMaterial() == null || shop.getStock() < amount) return false;
        ItemStack returned = new ItemStack(shop.getMaterial(), amount, shop.getData());
        if (!canFit(owner, returned)) return false;
        if (!owner.getInventory().addItem(returned).isEmpty()) return false;
        shop.setStock(shop.getStock() - amount);
        store.save();
        owner.updateInventory();
        return true;
    }

    public boolean configure(Player owner, UUID shopId, int amountPerSale, long priceCoins) {
        PlayerShop shop = store.get(shopId);
        if (owner == null || shop == null || !placementPolicy.canManageShop(owner, shop)) return false;
        if (amountPerSale < 1 || amountPerSale > 64 || priceCoins < 1) return false;
        shop.setAmountPerSale(amountPerSale);
        shop.setPriceCoins(priceCoins);
        store.save();
        return true;
    }

    public PlayerShopStore getStore() { return store; }

    private boolean canFit(Player player, ItemStack item) {
        Inventory temp = Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        return temp.addItem(item.clone()).isEmpty();
    }
}
