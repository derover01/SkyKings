package net.skykings.core.shop;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.logging.LoggingService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public long getCoinBalance(UUID uuid) { return uuid == null ? 0L : economyService.getBalance(uuid); }

    public boolean withdrawCoins(UUID uuid, long amount, String actor, String reason) {
        return uuid != null && amount > 0L && economyService.withdraw(uuid, amount, actor, reason);
    }

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
        inventory.clear();
        for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
        }
        if (inventory instanceof org.bukkit.inventory.PlayerInventory) {
            // updateInventory wird durch den aufrufenden Pfad am Ende/Fehlerfall erledigt.
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
}
