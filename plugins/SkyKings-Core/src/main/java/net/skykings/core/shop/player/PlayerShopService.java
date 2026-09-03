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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** Sichere Transaktionen fuer Villager-PlayerShops mit bis zu neun Trade-Spalten. */
public final class PlayerShopService {
    public enum Result { SUCCESS, NOT_ALLOWED, INVALID_SHOP, OUT_OF_STOCK, NOT_ENOUGH_MONEY, INVENTORY_FULL, FAILED }
    public static final class RevenueClaimOverflowException extends RuntimeException { public RevenueClaimOverflowException() { super("Coin balance would overflow while claiming PlayerShop revenue"); } }
    private static final int FEE_PERCENT = 5;
    private final PlayerShopStore store;
    private final EconomyService economy;
    private final LoggingService logging;
    private final PlayerShopPurchaseJournal purchaseJournal;
    private ShopPlacementPolicy placementPolicy;

    public PlayerShopService(PlayerShopStore store, EconomyService economy, LoggingService logging) {
        this(store, economy, logging, resolvePurchaseJournal());
    }

    PlayerShopService(PlayerShopStore store, EconomyService economy, LoggingService logging,
                      PlayerShopPurchaseJournal purchaseJournal) {
        this.store = store;
        this.economy = economy;
        this.logging = logging;
        this.purchaseJournal = purchaseJournal;
        this.placementPolicy = new ShopPlacementPolicy() {
            @Override public boolean canCreateShop(Player player, Location location) { return false; }
            @Override public boolean canManageShop(Player player, PlayerShop shop) { return player != null && shop != null && player.getUniqueId().equals(shop.getOwner()); }
            @Override public boolean canSellFromShop(PlayerShop shop) { return shop != null; }
        };
        ShopRentBootstrap.installLater(this);
    }

    private static PlayerShopPurchaseJournal resolvePurchaseJournal() {
        try {
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(PlayerShopService.class);
            if (plugin == null) return null;
            PlayerShopPurchaseJournal existing = PlayerShopPurchaseJournal.active();
            return existing != null ? existing : new PlayerShopPurchaseJournal(plugin);
        } catch (Throwable ignored) {
            // Isolierte Unit-Tests laufen ohne Bukkit-/Plugin-Kontext.
            return null;
        }
    }

    public void setPlacementPolicy(ShopPlacementPolicy placementPolicy) { if (placementPolicy != null) this.placementPolicy = placementPolicy; }
    public ShopPlacementPolicy getPlacementPolicy() { return placementPolicy; }
    public PlayerShopStore getStore() { return store; }

    public synchronized PlayerShop create(Player owner, Location location) {
        if (owner == null || location == null || !placementPolicy.canCreateShop(owner, location)) return null;
        PlayerShop shop = store.create(owner.getUniqueId()); if (shop == null) return null;
        shop.setWorld(location.getWorld() == null ? null : location.getWorld().getName()); shop.setX(location.getX()); shop.setY(location.getY()); shop.setZ(location.getZ());
        if (!store.saveChecked()) { store.delete(shop.getId()); return null; }
        return shop;
    }

    /** Legacy-Pfad: Angebot 1. */
    public synchronized Result purchase(Player buyer, UUID shopId) { return purchase(buyer, shopId, 0); }

    public synchronized Result purchase(Player buyer, UUID shopId, int offerIndex) {
        PlayerShop shop = store.get(shopId); PlayerShopOffer offer = shop == null ? null : shop.getOffer(offerIndex);
        if (buyer == null || shop == null || offer == null || !offer.isConfigured()) return Result.INVALID_SHOP;
        if (buyer.getUniqueId().equals(shop.getOwner())) return Result.NOT_ALLOWED;
        if (!PlayerShopTradeSnapshotRegistry.matchesIfPresent(buyer.getUniqueId(), shopId, offerIndex, offer)) return Result.INVALID_SHOP;
        if (!placementPolicy.canSellFromShop(shop)) return Result.NOT_ALLOWED;

        long price = offer.getPriceCoins();
        long fee = feeFor(price), sellerRevenue = price - fee, oldRevenue = shop.getPendingRevenue();
        if (sellerRevenue > 0L && oldRevenue > Long.MAX_VALUE - sellerRevenue) return Result.FAILED;
        if (sellerRevenue > 0L && !economy.canDeposit(shop.getOwner(), sellerRevenue)) return Result.FAILED;

        ItemStack top = offer.topStack(), middle = offer.middleStack();
        if (!canFit(buyer, top, middle)) return Result.INVENTORY_FULL;
        UUID buyerId = buyer.getUniqueId();
        if (!economy.has(buyerId, price)) return Result.NOT_ENOUGH_MONEY;

        Material material = offer.getMaterial();
        short data = offer.getData();
        int topAmount = offer.getAmountTop(), middleAmount = offer.getAmountMiddle();
        long oldPrice = offer.getPriceCoins();

        UUID transaction = null;
        if (purchaseJournal != null) {
            transaction = purchaseJournal.begin(shopId, offerIndex, buyerId, shop.getOwner(), material, data,
                    topAmount, middleAmount, price, sellerRevenue, oldRevenue);
            if (transaction == null) return Result.FAILED;
        }

        offer.clear();
        if (!store.saveChecked()) {
            restore(offer, material, data, topAmount, middleAmount, oldPrice);
            completeJournal(transaction, "RESERVATION_SAVE_REJECTED_BEFORE_EXTERNAL_MUTATION");
            return Result.FAILED;
        }

        if (!economy.withdraw(buyerId, price, "PLAYER_SHOP", "Kauf Shop " + shopId + " Angebot " + (offerIndex + 1))) {
            restore(offer, material, data, topAmount, middleAmount, oldPrice);
            if (store.saveChecked()) completeJournal(transaction, "WITHDRAW_REJECTED_AND_OFFER_RESTORED");
            else noteJournal(transaction, "OFFER_RESTORE_SAVE_FAILED_AFTER_WITHDRAW_REJECT");
            return Result.NOT_ENOUGH_MONEY;
        }

        if (purchaseJournal != null && !economy.persistNow(buyerId)) {
            boolean balanceRolledBack = rollbackBuyerBalance(buyerId, price, shopId);
            restore(offer, material, data, topAmount, middleAmount, oldPrice);
            boolean offerRestored = store.saveChecked();
            if (balanceRolledBack && offerRestored) completeJournal(transaction, "BUYER_DEBIT_COMMIT_FAILED_BUT_ROLLED_BACK");
            else noteJournal(transaction, "BUYER_DEBIT_COMMIT_FAILED_ROLLBACK_INCOMPLETE");
            return Result.FAILED;
        }

        if (!deliverAtomically(buyer, top, middle)) {
            boolean balanceRolledBack = rollbackBuyerBalance(buyerId, price, shopId);
            restore(offer, material, data, topAmount, middleAmount, oldPrice);
            boolean offerRestored = store.saveChecked();
            if (balanceRolledBack && offerRestored) completeJournal(transaction, "ITEM_DELIVERY_REJECTED_AND_ROLLED_BACK");
            else noteJournal(transaction, "ITEM_DELIVERY_ROLLBACK_INCOMPLETE");
            return Result.FAILED;
        }

        if (purchaseJournal != null) {
            try {
                buyer.saveData();
            } catch (RuntimeException ex) {
                noteJournal(transaction, "BUYER_PLAYER_DATA_SAVE_FAILED_AFTER_DELIVERY");
                buyer.updateInventory();
                return Result.FAILED;
            }
        }

        shop.setPendingRevenue(oldRevenue + sellerRevenue);
        boolean sellerRevenueCommitted = store.saveChecked();
        if (!sellerRevenueCommitted) {
            shop.setPendingRevenue(oldRevenue);
            if (!recoverSellerRevenue(shop.getOwner(), sellerRevenue, shopId)) {
                noteJournal(transaction, "SELLER_REVENUE_SAVE_AND_RECOVERY_FAILED");
                buyer.updateInventory();
                return Result.FAILED;
            }
        }

        if (purchaseJournal != null && !purchaseJournal.complete(transaction)) {
            purchaseJournal.noteFailure(transaction, "PURCHASE_COMMITTED_BUT_JOURNAL_COMPLETION_FAILED");
        }

        logging.log(new AuditEvent(AuditEventType.SHOP_PURCHASE, buyerId, "PLAYER_SHOP", price,
                "shop=" + shopId + ", offer=" + offerIndex + ", owner=" + shop.getOwner() + ", fee=" + fee
                        + ", item=" + material + ", amount=" + (topAmount + middleAmount)));
        buyer.updateInventory();
        return Result.SUCCESS;
    }

    private boolean rollbackBuyerBalance(UUID buyerId, long price, UUID shopId) {
        try {
            economy.deposit(buyerId, price, "PLAYER_SHOP_ROLLBACK", "Rollback Shop " + shopId);
            return purchaseJournal == null || economy.persistNow(buyerId);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean recoverSellerRevenue(UUID owner, long sellerRevenue, UUID shopId) {
        if (sellerRevenue <= 0L) return true;
        try {
            economy.deposit(owner, sellerRevenue, "PLAYER_SHOP_RECOVERY", "Revenue-Save-Recovery " + shopId);
            return purchaseJournal == null || economy.persistNow(owner);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void completeJournal(UUID transaction, String fallbackReason) {
        if (purchaseJournal == null || transaction == null) return;
        if (!purchaseJournal.complete(transaction)) purchaseJournal.noteFailure(transaction, fallbackReason);
    }

    private void noteJournal(UUID transaction, String reason) {
        if (purchaseJournal != null && transaction != null) purchaseJournal.noteFailure(transaction, reason);
    }

    public synchronized boolean putOfferStack(Player owner, UUID shopId, int offerIndex, boolean middleRow, ItemStack stack) {
        PlayerShop shop = store.get(shopId); PlayerShopOffer offer = shop == null ? null : shop.getOffer(offerIndex);
        if (owner == null || shop == null || offer == null || stack == null || stack.getType() == Material.AIR || !placementPolicy.canManageShop(owner, shop)) return false;
        if (stack.hasItemMeta() && stack.getItemMeta() != null && (stack.getItemMeta().hasDisplayName() || stack.getItemMeta().hasLore())) return false;
        if (!stack.getEnchantments().isEmpty()) return false;
        if (offer.getMaterial() != null && (offer.getMaterial() != stack.getType() || offer.getData() != stack.getDurability())) return false;
        Material oldMaterial = offer.getMaterial(); short oldData = offer.getData(); int oldTop = offer.getAmountTop(), oldMiddle = offer.getAmountMiddle();
        offer.setMaterial(stack.getType()); offer.setData(stack.getDurability());
        if (middleRow) offer.setAmountMiddle(stack.getAmount()); else offer.setAmountTop(stack.getAmount());
        if (!store.saveChecked()) { offer.setMaterial(oldMaterial); offer.setData(oldData); offer.setAmountTop(oldTop); offer.setAmountMiddle(oldMiddle); return false; }
        return true;
    }

    public synchronized ItemStack takeOfferStack(Player owner, UUID shopId, int offerIndex, boolean middleRow) {
        PlayerShop shop = store.get(shopId); PlayerShopOffer offer = shop == null ? null : shop.getOffer(offerIndex);
        if (owner == null || shop == null || offer == null || !placementPolicy.canManageShop(owner, shop)) return null;
        int amount = middleRow ? offer.getAmountMiddle() : offer.getAmountTop(); if (offer.getMaterial() == null || amount <= 0) return null;
        ItemStack result = new ItemStack(offer.getMaterial(), amount, offer.getData());
        if (!canFit(owner, result)) return null;
        int old = amount; if (middleRow) offer.setAmountMiddle(0); else offer.setAmountTop(0);
        if (offer.getTotalAmount() <= 0) offer.clear();
        if (!store.saveChecked()) {
            if (offer.getMaterial() == null) { offer.setMaterial(result.getType()); offer.setData(result.getDurability()); }
            if (middleRow) offer.setAmountMiddle(old); else offer.setAmountTop(old); return null;
        }
        return result;
    }

    public synchronized boolean setOfferPrice(Player owner, UUID shopId, int offerIndex, long price) {
        PlayerShop shop = store.get(shopId); PlayerShopOffer offer = shop == null ? null : shop.getOffer(offerIndex);
        if (owner == null || shop == null || offer == null || price < 1L || offer.getTotalAmount() <= 0 || !placementPolicy.canManageShop(owner, shop)) return false;
        long old = offer.getPriceCoins(); offer.setPriceCoins(price); if (store.saveChecked()) return true; offer.setPriceCoins(old); return false;
    }

    public synchronized long claimRevenue(Player owner, UUID shopId) {
        PlayerShop shop = store.get(shopId); if (owner == null || shop == null || !placementPolicy.canManageShop(owner, shop)) return 0L;
        long amount = shop.getPendingRevenue(); if (amount <= 0L) return 0L;
        if (!economy.canDeposit(owner.getUniqueId(), amount)) throw new RevenueClaimOverflowException();
        shop.setPendingRevenue(0L); if (!store.saveChecked()) { shop.setPendingRevenue(amount); return 0L; }
        economy.deposit(owner.getUniqueId(), amount, "PLAYER_SHOP", "Shop-Einnahmen " + shopId); return amount;
    }

    /* Legacy-Commands arbeiten weiter mit Angebot 1. */
    public synchronized boolean configure(Player owner, UUID shopId, int amount, long price) {
        PlayerShop shop = store.get(shopId); PlayerShopOffer offer = shop == null ? null : shop.getOffer(0);
        if (owner == null || offer == null || amount < 1 || amount > 128 || price < 1 || !placementPolicy.canManageShop(owner, shop)) return false;
        int oldTop = offer.getAmountTop(), oldMid = offer.getAmountMiddle(); long oldPrice = offer.getPriceCoins();
        offer.setAmountTop(Math.min(64, amount)); offer.setAmountMiddle(Math.max(0, amount - 64)); offer.setPriceCoins(price);
        if (store.saveChecked()) return true; offer.setAmountTop(oldTop); offer.setAmountMiddle(oldMid); offer.setPriceCoins(oldPrice); return false;
    }
    public synchronized boolean addStock(Player owner, UUID shopId, int amount) {
        if (amount < 1 || amount > 64) return false; ItemStack hand = owner == null ? null : owner.getItemInHand(); if (hand == null || hand.getAmount() < amount) return false;
        ItemStack deposit = hand.clone(); deposit.setAmount(amount); if (!putOfferStack(owner, shopId, 0, false, deposit)) return false;
        hand.setAmount(hand.getAmount() - amount); if (hand.getAmount() <= 0) owner.setItemInHand(new ItemStack(Material.AIR)); owner.updateInventory(); return true;
    }
    public synchronized boolean withdrawStock(Player owner, UUID shopId, int amount) {
        PlayerShop shop = store.get(shopId); PlayerShopOffer offer = shop == null ? null : shop.getOffer(0); if (offer == null || amount != offer.getAmountTop()) return false;
        ItemStack result = takeOfferStack(owner, shopId, 0, false); if (result == null) return false; owner.getInventory().addItem(result); owner.updateInventory(); return true;
    }

    private void restore(PlayerShopOffer offer, Material material, short data, int top, int middle, long price) { offer.setMaterial(material); offer.setData(data); offer.setAmountTop(top); offer.setAmountMiddle(middle); offer.setPriceCoins(price); }

    /**
     * Zustellung beider Trade-Stacks als eine Einheit. Der Preflight sollte bereits garantieren,
     * dass alles passt. Falls Bukkit trotzdem Restitems meldet, wird das Storage-Inventar exakt
     * auf den Zustand vor der Zustellung zurueckgesetzt, bevor Economy/Offer-Rollback erfolgt.
     */
    private boolean deliverAtomically(Player player, ItemStack... items) {
        ItemStack[] before = new ItemStack[36];
        for (int i = 0; i < before.length; i++) {
            ItemStack current = player.getInventory().getItem(i);
            before[i] = current == null ? null : current.clone();
        }
        for (ItemStack item : items) {
            if (item == null) continue;
            if (!player.getInventory().addItem(item.clone()).isEmpty()) {
                for (int i = 0; i < before.length; i++) {
                    player.getInventory().setItem(i, before[i] == null ? null : before[i].clone());
                }
                player.updateInventory();
                return false;
            }
        }
        return true;
    }

    private boolean canFit(Player player, ItemStack... items) {
        Inventory temp = Bukkit.createInventory(null, 36); for (int i = 0; i < 36; i++) { ItemStack current = player.getInventory().getItem(i); if (current != null) temp.setItem(i, current.clone()); }
        for (ItemStack item : items) if (item != null && !temp.addItem(item.clone()).isEmpty()) return false; return true;
    }
    private long feeFor(long price) { long whole = price / 100L, rem = price % 100L; return Math.max(1L, whole * FEE_PERCENT + (rem * FEE_PERCENT / 100L)); }
}
