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

    /**
     * Claim bleibt unangetastet, wenn die Auszahlung den Coin-Kontostand ueberlaufen lassen wuerde.
     * Der Controller kann diesen seltenen Fall dadurch klar vom leeren Einnahmenkonto unterscheiden.
     */
    public static final class RevenueClaimOverflowException extends RuntimeException {
        public RevenueClaimOverflowException() {
            super("Coin balance would overflow while claiming PlayerShop revenue");
        }
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

        long price = shop.getPriceCoins();
        long fee = feeFor(price);
        long sellerRevenue = price - fee;
        long oldRevenue = shop.getPendingRevenue();
        // Pending-Revenue niemals saturieren oder still abschneiden: vor dem Kauf fail-closed.
        if (sellerRevenue > 0L && oldRevenue > Long.MAX_VALUE - sellerRevenue) return Result.FAILED;

        ItemStack reward = new ItemStack(shop.getMaterial(), amount, shop.getData());
        if (!canFit(buyer, reward)) return Result.INVENTORY_FULL;
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

        shop.setPendingRevenue(oldRevenue + sellerRevenue);
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

        // Wichtig: Overflow pruefen, BEVOR pendingRevenue persistent auf 0 reserviert wird.
        // EconomyService.deposit() wuerde in diesem Fall ebenfalls vor der Profilmutation abbrechen,
        // aber zu diesem Zeitpunkt waere der Shop-Claim sonst bereits dauerhaft als verbraucht markiert.
        try {
            Math.addExact(economy.getBalance(owner.getUniqueId()), amount);
        } catch (ArithmeticException ex) {
            throw new RevenueClaimOverflowException();
        }

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
        int oldStock = shop.getStock();
        // Auch bei theoretisch sehr grossem Langzeitbestand niemals int-Wraparound zulassen.
        // Ein negativer Bestand koennte sonst nach genug Einzahlungen persistiert werden.
        if (oldStock < 0 || oldStock > Integer.MAX_VALUE - amount) return false;

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

    private long feeFor(long price) {
        // Vermeidet den Overflow von "price * FEE_PERCENT" bei sehr grossen long-Werten.
        long wholeHundreds = price / 100L;
        long remainder = price % 100L;
        long fee = wholeHundreds * FEE_PERCENT + (remainder * FEE_PERCENT / 100L);
        return Math.max(1L, fee);
    }
}
