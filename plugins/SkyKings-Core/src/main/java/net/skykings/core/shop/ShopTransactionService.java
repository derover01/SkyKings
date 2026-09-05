package net.skykings.core.shop;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.logging.LoggingService;
import net.skykings.core.permission.VoucherPermission;
import net.skykings.core.permission.VoucherPermissionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Weltunabhaengige Transaktionslogik fuer System-, Villager- und Player-Shops. */
public final class ShopTransactionService {

    private final EconomyService economyService;
    private final LoggingService loggingService;
    private final ShopSettlementJournal settlementJournal;

    public ShopTransactionService(EconomyService economyService, LoggingService loggingService) {
        this(economyService, loggingService, resolveJournal());
    }

    ShopTransactionService(EconomyService economyService, LoggingService loggingService,
                           ShopSettlementJournal settlementJournal) {
        this.economyService = economyService;
        this.loggingService = loggingService;
        this.settlementJournal = settlementJournal;
    }

    private static ShopSettlementJournal resolveJournal() {
        try {
            ShopSettlementJournal existing = ShopSettlementJournal.active();
            if (existing != null) return existing;
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(ShopTransactionService.class);
            return plugin == null ? null : new ShopSettlementJournal(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public synchronized ShopPurchaseResult purchase(Player player, ShopOffer offer, String shopId) {
        if (player == null || offer == null) return ShopPurchaseResult.INVALID_OFFER;
        UUID playerId = player.getUniqueId();
        if (settlementJournal != null && settlementJournal.hasPendingFor(playerId)) return ShopPurchaseResult.FAILED;

        ItemStack reward = offer.getItem();
        if (reward.getType() == Material.AIR || reward.getAmount() <= 0) return ShopPurchaseResult.INVALID_OFFER;
        if (!canFit(player, reward)) return ShopPurchaseResult.INVENTORY_FULL;

        if (offer.getCurrency() != ShopCurrency.COINS && offer.getCurrency() != ShopCurrency.NETHERSTARS) {
            return ShopPurchaseResult.INVALID_OFFER;
        }
        if (offer.getCurrency() == ShopCurrency.NETHERSTARS && offer.getPrice() > Integer.MAX_VALUE) {
            return ShopPurchaseResult.INVALID_OFFER;
        }

        String normalizedShop = safeShopId(shopId);
        UUID transaction = settlementJournal == null ? null : settlementJournal.begin(
                playerId, normalizedShop, offer.getId(), offer.getCurrency().name(), offer.getPrice(),
                reward.getType().name(), reward.getAmount());
        if (settlementJournal != null && transaction == null) return ShopPurchaseResult.FAILED;

        Map<Integer, ItemStack> inventoryBefore = snapshot(player.getInventory());

        if (offer.getCurrency() == ShopCurrency.COINS) {
            if (!economyService.has(playerId, offer.getPrice())) {
                completeJournal(transaction, "BALANCE_CHECK_REJECTED_BEFORE_MUTATION");
                return ShopPurchaseResult.NOT_ENOUGH_MONEY;
            }
            boolean withdrawn = economyService.withdraw(playerId, offer.getPrice(), "SHOP",
                    "Shop " + normalizedShop + " / " + offer.getId());
            if (!withdrawn) {
                completeJournal(transaction, "WITHDRAW_REJECTED_BEFORE_MUTATION");
                return ShopPurchaseResult.NOT_ENOUGH_MONEY;
            }
            if (!player.getInventory().addItem(reward.clone()).isEmpty()) {
                restore(player.getInventory(), inventoryBefore);
                try {
                    economyService.deposit(playerId, offer.getPrice(), "SHOP_ROLLBACK",
                            "Rollback " + normalizedShop + " / " + offer.getId());
                } catch (RuntimeException ex) {
                    noteJournal(transaction, "ROLLBACK_REFUND_MUTATION_FAILED");
                    return ShopPurchaseResult.FAILED;
                }
                if (settlementJournal != null) {
                    if (!economyService.persistNow(playerId) || !savePlayerData(player)) {
                        noteJournal(transaction, "ROLLBACK_DURABLE_COMMIT_FAILED");
                        return ShopPurchaseResult.FAILED;
                    }
                    if (!closeJournal(transaction)) return ShopPurchaseResult.FAILED;
                }
                return ShopPurchaseResult.FAILED;
            }

            if (settlementJournal != null) {
                if (!economyService.persistNow(playerId)) {
                    noteJournal(transaction, "COIN_BALANCE_DURABLE_COMMIT_FAILED_AFTER_REWARD");
                    return ShopPurchaseResult.FAILED;
                }
                if (!savePlayerData(player)) {
                    noteJournal(transaction, "REWARD_PLAYERDATA_COMMIT_FAILED_AFTER_COIN_COMMIT");
                    return ShopPurchaseResult.FAILED;
                }
                if (!closeJournal(transaction)) return ShopPurchaseResult.FAILED;
            }
        } else {
            int cost = (int) offer.getPrice();
            if (countNetherstars(player.getInventory()) < cost) {
                completeJournal(transaction, "STAR_BALANCE_REJECTED_BEFORE_MUTATION");
                return ShopPurchaseResult.NOT_ENOUGH_NETHERSTARS;
            }
            removeNetherstars(player.getInventory(), cost);
            if (!player.getInventory().addItem(reward.clone()).isEmpty()) {
                restore(player.getInventory(), inventoryBefore);
                if (settlementJournal != null) {
                    if (!savePlayerData(player)) {
                        noteJournal(transaction, "STAR_ROLLBACK_PLAYERDATA_COMMIT_FAILED");
                        return ShopPurchaseResult.FAILED;
                    }
                    if (!closeJournal(transaction)) return ShopPurchaseResult.FAILED;
                }
                return ShopPurchaseResult.FAILED;
            }
            if (settlementJournal != null) {
                if (!savePlayerData(player)) {
                    noteJournal(transaction, "STAR_PURCHASE_PLAYERDATA_COMMIT_FAILED");
                    return ShopPurchaseResult.FAILED;
                }
                if (!closeJournal(transaction)) return ShopPurchaseResult.FAILED;
            }
        }

        loggingService.log(new AuditEvent(AuditEventType.SHOP_PURCHASE,
                playerId, "SHOP", offer.getPrice(),
                "shop=" + normalizedShop + ", offer=" + offer.getId() + ", currency=" + offer.getCurrency()
                        + ", item=" + reward.getType() + ", amount=" + reward.getAmount()));
        player.updateInventory();
        return ShopPurchaseResult.SUCCESS;
    }

    /**
     * Verkauft exakt die angegebenen Inventar-Slots gegen Coins. Der Item-Teil wird zuerst
     * synchron in player.dat persistiert, danach erst die Coin-Gutschrift durable gemacht.
     * Ein Crash zwischen beiden Stores bleibt dadurch als Journal-Review sichtbar statt
     * Items und Coins automatisch ein zweites Mal zu vergeben.
     */
    public synchronized ShopSaleResult sell(Player player, Map<Integer, ItemStack> soldSlots,
                                            long payout, String saleId, String reason) {
        if (player == null || soldSlots == null || soldSlots.isEmpty() || payout <= 0L) {
            return ShopSaleResult.INVALID_SALE;
        }
        UUID playerId = player.getUniqueId();
        if (settlementJournal == null) return ShopSaleResult.FAILED;
        if (settlementJournal.hasPendingFor(playerId)) return ShopSaleResult.REVIEW_REQUIRED;
        if (!economyService.canDeposit(playerId, payout)) return ShopSaleResult.BALANCE_OVERFLOW;

        int soldItemCount = 0;
        for (Map.Entry<Integer, ItemStack> entry : soldSlots.entrySet()) {
            Integer slot = entry.getKey();
            ItemStack expected = entry.getValue();
            if (slot == null || slot < 0 || slot >= player.getInventory().getSize()
                    || expected == null || expected.getType() == Material.AIR || expected.getAmount() <= 0) {
                return ShopSaleResult.INVALID_SALE;
            }
            ItemStack current = player.getInventory().getItem(slot);
            if (current == null || current.getAmount() != expected.getAmount() || !current.isSimilar(expected)) {
                return ShopSaleResult.STALE_INVENTORY;
            }
            if (Integer.MAX_VALUE - soldItemCount < expected.getAmount()) return ShopSaleResult.INVALID_SALE;
            soldItemCount += expected.getAmount();
        }

        String normalizedSale = safeSaleId(saleId);
        UUID transaction = settlementJournal.beginSale(playerId, normalizedSale, payout, soldItemCount);
        if (transaction == null) return ShopSaleResult.FAILED;

        for (Integer slot : soldSlots.keySet()) player.getInventory().setItem(slot, null);

        // Wichtig: Item-Entfernung zuerst durable. Bei einem Hard-Crash danach darf der Server
        // nicht mit alten Items plus bereits gutgeschriebenen Coins wieder hochkommen.
        if (!savePlayerData(player)) {
            restoreSlots(player.getInventory(), soldSlots);
            if (!savePlayerData(player)) {
                noteJournal(transaction, "SALE_ITEM_ROLLBACK_PLAYERDATA_COMMIT_FAILED");
                return ShopSaleResult.REVIEW_REQUIRED;
            }
            if (!closeJournal(transaction)) return ShopSaleResult.REVIEW_REQUIRED;
            return ShopSaleResult.FAILED;
        }

        try {
            economyService.deposit(playerId, payout, "SYSTEM_SHOP_SELL",
                    reason == null ? normalizedSale : reason);
        } catch (RuntimeException ex) {
            noteJournal(transaction, "SALE_COIN_DEPOSIT_MUTATION_FAILED_AFTER_ITEM_COMMIT");
            return ShopSaleResult.REVIEW_REQUIRED;
        }
        if (!economyService.persistNow(playerId)) {
            noteJournal(transaction, "SALE_COIN_DURABLE_COMMIT_FAILED_AFTER_ITEM_COMMIT");
            return ShopSaleResult.REVIEW_REQUIRED;
        }
        if (!closeJournal(transaction)) return ShopSaleResult.REVIEW_REQUIRED;

        loggingService.log(new AuditEvent(AuditEventType.SHOP_SALE,
                playerId, "SYSTEM_SHOP_SELL", payout,
                "sale=" + normalizedSale + ", items=" + soldItemCount));
        player.updateInventory();
        return ShopSaleResult.SUCCESS;
    }

    /** Fail-closed Blacksmith-Settlement ueber Coin-Profil und player.dat. */
    public synchronized ShopPurchaseResult repairHeldItem(Player player, ItemStack expected, long price, String repairId) {
        if (player == null || expected == null || expected.getType() == Material.AIR || price <= 0L) {
            return ShopPurchaseResult.INVALID_OFFER;
        }
        UUID playerId = player.getUniqueId();
        if (settlementJournal == null || settlementJournal.hasPendingFor(playerId)) return ShopPurchaseResult.FAILED;

        int slot = player.getInventory().getHeldItemSlot();
        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || current.getAmount() != expected.getAmount() || !current.isSimilar(expected)
                || current.getDurability() <= 0 || current.getType().getMaxDurability() <= 0) {
            return ShopPurchaseResult.INVALID_OFFER;
        }
        if (!economyService.has(playerId, price)) return ShopPurchaseResult.NOT_ENOUGH_MONEY;

        String normalizedRepair = repairId == null || repairId.trim().isEmpty() ? "BLACKSMITH_REPAIR" : repairId.trim();
        UUID transaction = settlementJournal.begin(playerId, "BLACKSMITH", normalizedRepair, "COINS", price,
                current.getType().name(), 1);
        if (transaction == null) return ShopPurchaseResult.FAILED;

        if (!economyService.withdraw(playerId, price, "BLACKSMITH", "Repair " + current.getType())) {
            completeJournal(transaction, "BLACKSMITH_WITHDRAW_REJECTED_BEFORE_MUTATION");
            return ShopPurchaseResult.NOT_ENOUGH_MONEY;
        }

        ItemStack damagedSnapshot = current.clone();
        ItemStack repaired = current.clone();
        repaired.setDurability((short) 0);
        player.getInventory().setItem(slot, repaired);

        // Coins zuerst durable. Ein Crash danach kann eine bezahlte, aber noch nicht durable
        // Reparatur erzeugen; das Journal bleibt dann Review, niemals eine kostenlose Reparatur.
        if (!economyService.persistNow(playerId)) {
            player.getInventory().setItem(slot, damagedSnapshot);
            savePlayerData(player);
            noteJournal(transaction, "BLACKSMITH_COIN_DURABLE_COMMIT_FAILED");
            return ShopPurchaseResult.FAILED;
        }
        if (!savePlayerData(player)) {
            player.getInventory().setItem(slot, damagedSnapshot);
            savePlayerData(player);
            noteJournal(transaction, "BLACKSMITH_REPAIR_PLAYERDATA_COMMIT_FAILED_AFTER_COIN_COMMIT");
            return ShopPurchaseResult.FAILED;
        }
        if (!closeJournal(transaction)) return ShopPurchaseResult.FAILED;

        loggingService.log(new AuditEvent(AuditEventType.SHOP_PURCHASE,
                playerId, "BLACKSMITH", price,
                "repair=" + current.getType() + ", old-durability=" + damagedSnapshot.getDurability()));
        player.updateInventory();
        return ShopPurchaseResult.SUCCESS;
    }

    /**
     * Permanentes Recht: Coins werden zuerst durable abgebucht, erst danach wird der Permission-Grant
     * persistent gespeichert. Ein fehlgeschlagener/unklarer Grant wird bewusst NICHT automatisch
     * refunded, weil LuckPerms den Node bereits angewendet, aber den Save als fehlgeschlagen melden
     * koennte. Das offene Journal zwingt dann Staff-Review statt eines moeglichen Free-Permission-Dupes.
     */
    public CompletableFuture<ShopPurchaseResult> purchasePermission(final Player player,
                                                                     final VoucherPermissionService rights,
                                                                     final VoucherPermission right,
                                                                     final long price) {
        if (player == null || rights == null || right == null || price <= 0L) {
            return CompletableFuture.completedFuture(ShopPurchaseResult.INVALID_OFFER);
        }
        final UUID playerId = player.getUniqueId();
        final UUID transaction;
        synchronized (this) {
            if (settlementJournal == null || settlementJournal.hasPendingFor(playerId)) {
                return CompletableFuture.completedFuture(ShopPurchaseResult.FAILED);
            }
            if (player.hasPermission(right.getNode())) {
                return CompletableFuture.completedFuture(ShopPurchaseResult.SUCCESS);
            }
            if (!economyService.has(playerId, price)) {
                return CompletableFuture.completedFuture(ShopPurchaseResult.NOT_ENOUGH_MONEY);
            }
            transaction = settlementJournal.begin(playerId, "SYSTEM_RIGHTS", right.getId(), "COINS", price,
                    "PERMISSION", 1);
            if (transaction == null) return CompletableFuture.completedFuture(ShopPurchaseResult.FAILED);
            if (!economyService.withdraw(playerId, price, "SHOP_RIGHT", "Recht " + right.getId())) {
                completeJournal(transaction, "RIGHT_WITHDRAW_REJECTED_BEFORE_MUTATION");
                return CompletableFuture.completedFuture(ShopPurchaseResult.NOT_ENOUGH_MONEY);
            }
            if (!economyService.persistNow(playerId)) {
                noteJournal(transaction, "RIGHT_COIN_DURABLE_COMMIT_FAILED_BEFORE_PERMISSION");
                return CompletableFuture.completedFuture(ShopPurchaseResult.FAILED);
            }
        }

        try {
            return rights.grantDurably(playerId, right.getId(), "SHOP_RIGHT").handle((status, throwable) -> {
                synchronized (ShopTransactionService.this) {
                    if (throwable != null || status != VoucherPermissionService.GrantStatus.GRANTED) {
                        noteJournal(transaction, throwable == null
                                ? "RIGHT_PERMISSION_DURABLE_GRANT_REJECTED_AFTER_COIN_COMMIT"
                                : "RIGHT_PERMISSION_DURABLE_GRANT_EXCEPTION_AFTER_COIN_COMMIT");
                        return ShopPurchaseResult.FAILED;
                    }
                    if (!closeJournal(transaction)) return ShopPurchaseResult.FAILED;
                    loggingService.log(new AuditEvent(AuditEventType.SHOP_PURCHASE,
                            playerId, "SHOP_RIGHT", price,
                            "permission=" + right.getId() + ", node=" + right.getNode()));
                    return ShopPurchaseResult.SUCCESS;
                }
            });
        } catch (RuntimeException ex) {
            synchronized (this) {
                noteJournal(transaction, "RIGHT_PERMISSION_GRANT_START_FAILED_AFTER_COIN_COMMIT");
            }
            return CompletableFuture.completedFuture(ShopPurchaseResult.FAILED);
        }
    }

    public long getCoinBalance(UUID uuid) { return uuid == null ? 0L : economyService.getBalance(uuid); }

    /** Legacy-Helfer fuer nicht-journaled Sonderpfade; neue Cross-Store-Kaeufe sollen eigene Settlement-Methoden verwenden. */
    public boolean withdrawCoins(UUID uuid, long amount, String actor, String reason) {
        return uuid != null && amount > 0L && economyService.withdraw(uuid, amount, actor, reason);
    }

    /** Legacy-Helfer fuer nicht-journaled Sonderpfade; nicht fuer automatische Ambiguitaets-Refunds verwenden. */
    public void depositCoins(UUID uuid, long amount, String actor, String reason) {
        if (uuid != null && amount > 0L) economyService.deposit(uuid, amount, actor, reason);
    }

    public int countNetherstars(Inventory inventory) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.NETHER_STAR) total += item.getAmount();
        }
        return total;
    }

    private void removeNetherstars(Inventory inventory, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() != Material.NETHER_STAR) continue;
            int take = Math.min(item.getAmount(), remaining);
            int newAmount = item.getAmount() - take;
            if (newAmount <= 0) inventory.setItem(slot, null);
            else { item.setAmount(newAmount); inventory.setItem(slot, item); }
            remaining -= take;
        }
    }

    private boolean canFit(Player player, ItemStack reward) {
        Inventory temp = org.bukkit.Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current != null) temp.setItem(i, current.clone());
        }
        return temp.addItem(reward.clone()).isEmpty();
    }

    private Map<Integer, ItemStack> snapshot(Inventory inventory) {
        Map<Integer, ItemStack> result = new HashMap<Integer, ItemStack>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) result.put(i, item.clone());
        }
        return result;
    }

    private void restore(Inventory inventory, Map<Integer, ItemStack> snapshot) {
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, null);
        for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
        }
    }

    private void restoreSlots(Inventory inventory, Map<Integer, ItemStack> snapshot) {
        for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
        }
    }

    private boolean savePlayerData(Player player) {
        try {
            player.updateInventory();
            player.saveData();
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void completeJournal(UUID transaction, String fallbackReason) {
        if (settlementJournal == null || transaction == null) return;
        if (!settlementJournal.complete(transaction)) settlementJournal.noteFailure(transaction, fallbackReason);
    }

    private boolean closeJournal(UUID transaction) {
        if (settlementJournal == null || transaction == null) return true;
        if (settlementJournal.complete(transaction)) return true;
        settlementJournal.noteFailure(transaction, "SETTLEMENT_COMMITTED_BUT_JOURNAL_CLOSE_FAILED");
        return false;
    }

    private void noteJournal(UUID transaction, String reason) {
        if (settlementJournal != null && transaction != null) settlementJournal.noteFailure(transaction, reason);
    }

    private String safeShopId(String shopId) { return shopId == null || shopId.trim().isEmpty() ? "unknown" : shopId.trim(); }
    private String safeSaleId(String saleId) { return saleId == null || saleId.trim().isEmpty() ? "unknown-sale" : saleId.trim(); }
}
