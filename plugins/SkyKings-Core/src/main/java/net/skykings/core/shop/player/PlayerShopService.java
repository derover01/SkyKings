package net.skykings.core.shop.player;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.logging.LoggingService;
import net.skykings.core.shop.rent.ShopRentBootstrap;
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
            @Override public boolean canSellFromShop(PlayerShop shop) { return shop != null; }
        };
        ShopRentBootstrap.installLater(this);
    }

    public void setPlacementPolicy(ShopPlacementPolicy placementPolicy) {
        if (placementPolicy != null) this.placementPolicy = placementPolicy;
    }

    public ShopPlacementPolicy getPlacementPolicy() { return placementPolicy; }

    public synchronized PlayerShop create(Player owner, Location location) {
        if (owner == null || location == null || !placementPolicy.canCreateShop(owner, location)) return null;
        PlayerShop shop = store.create(owner.getUniqueId());
        if (shop == null) return null;
        shop.setWorld(location.getWorld() == null ? null : location.getWorld().getName());
        shop.setX(location.getX()); shop.setY(location.getY()); shop.setZ(location.getZ());
        if (!store.saveChecked()) {
            store.delete(shop.getId());
            return null;
        }
        return shop;
    }

    public synchronized Result purchase(Player buyer, UUID shopId) {
        PlayerShop shop = store.get(shopId);
        if (buyer == null || shop == null || !shop.isConfigured()) return Result.INVALID_SHOP;
        if (!placementPolicy.canSellFromShop(shop)) return Result.NOT_ALLOWED;
        final int amount = shop.getAmountPerSale();
        final int oldStock = shop.getStock();
        if (oldStock < amount) return Result.OUT_OF_STOCK;

        ItemStack reward = new ItemStack(shop.getMaterial(), amount, shop.getData());
        if (!canFit(buyer, reward)) return Result.INVENTORY_FULL;
        long price = shop.getPriceCoins();
        if (!economy.has(buyer.getUniqueId(), price)) return Result.NOT_ENOUGH_MONEY;

        // Stock wird persistent reserviert, bevor Coins oder Items die Seite wechseln.
        shop.setStock(oldStock - amount);
        if (!store.saveChecked()) {
            shop.setStock(oldStock);
            return Result.FAILED;
        }

        if (!economy.withdraw(buyer.getUniqueId(), price, "PLAYER_SHOP", "Kauf Shop " + shopId)) {
            shop.setStock(oldStock);
            store.saveChecked();
            return Result.NOT_ENOUGH_MONEY;
        }

        if (!buyer.getInventory().addItem(reward).isEmpty()) {
            economy.deposit(buyer.getUniqueId(), price, "PLAYER_SHOP_ROLLBACK", "Rollback Shop " + shopId);
            shop.setStock(oldStock);
            store.saveChecked();
            return Result.FAILED;
        }

        long fee = Math.max(1L, price * FEE_PERCENT / 100L);
        long sellerRevenue = Math.max(0L, price - fee);
        long oldRevenue = shop.getPendingRevenue();
        shop.setPendingRevenue(safeAdd(oldRevenue, sellerRevenue));
        if (!store.saveChecked()) {
            // Der Kauf ist zu diesem Zeitpunkt bereits abgeschlossen. Die atomare Store-Save-Methode
            // garantiert bei false, dass der alte Dateistand bestehen blieb. Deshalb den In-Memory-
            // Wert auf genau diesen sicheren Stand zuruecksetzen und nur den NEUEN Verkaeuferanteil
            // direkt auszahlen. So kann ein spaeterer Save denselben Betrag nicht erneut als Pending
            // Revenue persistieren.
            shop.setPendingRevenue(oldRevenue);
            economy.deposit(shop.getOwner(), sellerRevenue, "PLAYER_SHOP_RECOVERY",
                    "Direktauszahlung nach Revenue-Save-Fehler " + shopId);
            logging.log(new AuditEvent(AuditEventType.SHOP_PURCHASE, buyer.getUniqueId(), "PLAYER_SHOP_SAVE_RECOVERY", sellerRevenue,
                    "shop=" + shopId + ", owner=" + shop.getOwner() + ", pendingRevenuePersistFailed=true, directSellerPayout=true"));
        }

        logging.log(new AuditEvent(AuditEventType.SHOP_PURCHASE, buyer.getUniqueId(), "PLAYER_SHOP", price,
                "shop=" + shopId + ", owner=" + shop.getOwner() + ", fee=" + fee + ", item=" + shop.getMaterial()
                        + ", amount=" + amount));
        buyer.updateInventory();
        return Result.SUCCESS;
    }

    public synchronized long claimRevenue(Player owner, UUID shopId) {
        PlayerShop shop = store.get(shopId);
        if (owner == null || shop == null || !placementPolicy.canManageShop(owner, shop)) return 0L;
        long amount = shop.getPendingRevenue();
        if (amount <= 0L) return 0L;
        shop.setPendingRevenue(0L);
        if (!store.saveChecked()) {
            shop.setPendingRevenue(amount);
            return 0L;
        }
        economy.deposit(owner.getUniqueId(), amount, "PLAYER_SHOP", "Shop-Einnahmen " + shopId);
        return amount;
    }

    public synchronized boolean addStock(Player owner, UUID shopId, int amount) {
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

        ItemStack originalHand = hand.clone();
        int oldStock = shop.getStock();
        int remaining = hand.getAmount() - amount;
        if (remaining <= 0) owner.setItemInHand(new ItemStack(Material.AIR));
        else { hand.setAmount(remaining); owner.setItemInHand(hand); }
        shop.setStock(oldStock + amount);
        if (!store.saveChecked()) {
            shop.setStock(oldStock);
            owner.setItemInHand(originalHand);
            owner.updateInventory();
            return false;
        }
        owner.updateInventory();
        return true;
    }

    public synchronized boolean withdrawStock(Player owner, UUID shopId, int amount) {
        PlayerShop shop = store.get(shopId);
        if (owner == null || shop == null || amount <= 0 || !placementPolicy.canManageShop(owner, shop)) return false;
        if (shop.getMaterial() == null || shop.getStock() < amount) return false;
        ItemStack returned = new ItemStack(shop.getMaterial(), amount, shop.getData());
        if (!canFit(owner, returned)) return false;

        int oldStock = shop.getStock();
        shop.setStock(oldStock - amount);
        if (!store.saveChecked()) {
            shop.setStock(oldStock);
            return false;
        }

        if (!owner.getInventory().addItem(returned).isEmpty()) {
            shop.setStock(oldStock);
            store.saveChecked();
            return false;
        }
        owner.updateInventory();
        return true;
    }

    public synchronized boolean configure(Player owner, UUID shopId, int amountPerSale, long priceCoins) {
        PlayerShop shop = store.get(shopId);
        if (owner == null || shop == null || !placementPolicy.canManageShop(owner, shop)) return false;
        if (amountPerSale < 1 || amountPerSale > 64 || priceCoins < 1) return false;
        int oldAmount = shop.getAmountPerSale();
        long oldPrice = shop.getPriceCoins();
        shop.setAmountPerSale(amountPerSale);
        shop.setPriceCoins(priceCoins);
        if (!store.saveChecked()) {
            shop.setAmountPerSale(oldAmount);
            shop.setPriceCoins(oldPrice);
            return false;
        }
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

    private long safeAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }
}
