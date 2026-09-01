package net.skykings.core.shop;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.logging.LoggingService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Weltunabhaengige Transaktionslogik fuer System-, Villager- und Player-Shops. */
public final class ShopTransactionService {

    private final EconomyService economyService;
    private final LoggingService loggingService;

    public ShopTransactionService(EconomyService economyService, LoggingService loggingService) {
        this.economyService = economyService;
        this.loggingService = loggingService;
    }

    public ShopPurchaseResult purchase(Player player, ShopOffer offer, String shopId) {
        if (player == null || offer == null) return ShopPurchaseResult.INVALID_OFFER;
        ItemStack reward = offer.getItem();
        if (reward.getType() == Material.AIR || reward.getAmount() <= 0) return ShopPurchaseResult.INVALID_OFFER;
        if (!canFit(player, reward)) return ShopPurchaseResult.INVENTORY_FULL;

        if (offer.getCurrency() == ShopCurrency.COINS) {
            if (!economyService.has(player.getUniqueId(), offer.getPrice())) return ShopPurchaseResult.NOT_ENOUGH_MONEY;
            boolean withdrawn = economyService.withdraw(player.getUniqueId(), offer.getPrice(), "SHOP", "Shop " + safeShopId(shopId) + " / " + offer.getId());
            if (!withdrawn) return ShopPurchaseResult.NOT_ENOUGH_MONEY;
            if (!player.getInventory().addItem(reward).isEmpty()) {
                economyService.deposit(player.getUniqueId(), offer.getPrice(), "SHOP_ROLLBACK", "Rollback " + safeShopId(shopId) + " / " + offer.getId());
                return ShopPurchaseResult.FAILED;
            }
        } else if (offer.getCurrency() == ShopCurrency.NETHERSTARS) {
            if (offer.getPrice() > Integer.MAX_VALUE) return ShopPurchaseResult.INVALID_OFFER;
            int cost = (int) offer.getPrice();
            if (countNetherstars(player.getInventory()) < cost) return ShopPurchaseResult.NOT_ENOUGH_NETHERSTARS;
            Map<Integer, ItemStack> snapshot = snapshot(player.getInventory());
            removeNetherstars(player.getInventory(), cost);
            if (!player.getInventory().addItem(reward).isEmpty()) {
                restore(player.getInventory(), snapshot);
                return ShopPurchaseResult.FAILED;
            }
        } else return ShopPurchaseResult.INVALID_OFFER;

        loggingService.log(new AuditEvent(AuditEventType.SHOP_PURCHASE,
                player.getUniqueId(), "SHOP", offer.getPrice(),
                "shop=" + safeShopId(shopId) + ", offer=" + offer.getId() + ", currency=" + offer.getCurrency()
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
        for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) inventory.setItem(entry.getKey(), entry.getValue());
    }

    private String safeShopId(String shopId) { return shopId == null || shopId.trim().isEmpty() ? "unknown" : shopId.trim(); }
}
