package net.skykings.core.shop.player;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.LoggingService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerShopDurablePurchaseTest {

    @Test
    public void successfulPurchaseCommitsBuyerAndCompletesJournal() {
        Fixture f = fixture();
        when(f.economy.persistNow(f.buyerId)).thenReturn(true);
        when(f.journal.complete(f.transaction)).thenReturn(true);

        PlayerShopService.Result result = purchase(f);

        assertEquals(PlayerShopService.Result.SUCCESS, result);
        assertEquals(0, f.shop.getOffer(0).getTotalAmount());
        assertEquals(1_050L, f.shop.getPendingRevenue());
        verify(f.economy).persistNow(f.buyerId);
        verify(f.buyer).saveData();
        verify(f.journal).complete(f.transaction);
    }

    @Test
    public void ambiguousPlayerDataSaveLeavesJournalForReviewWithoutRefund() {
        Fixture f = fixture();
        when(f.economy.persistNow(f.buyerId)).thenReturn(true);
        doThrow(new RuntimeException("player.dat unavailable")).when(f.buyer).saveData();

        PlayerShopService.Result result = purchase(f);

        assertEquals(PlayerShopService.Result.FAILED, result);
        assertEquals(0, f.shop.getOffer(0).getTotalAmount());
        assertEquals(100L, f.shop.getPendingRevenue());
        verify(f.journal).noteFailure(f.transaction, "BUYER_PLAYER_DATA_SAVE_FAILED_AFTER_DELIVERY");
        verify(f.journal, never()).complete(f.transaction);
        verify(f.economy, never()).deposit(f.buyerId, 1_000L, "PLAYER_SHOP_ROLLBACK", "Rollback Shop " + f.shopId);
    }

    @Test
    public void failedBuyerDebitCommitRollsBackBalanceAndOfferDurably() {
        Fixture f = fixture();
        when(f.economy.persistNow(f.buyerId)).thenReturn(false, true);
        when(f.journal.complete(f.transaction)).thenReturn(true);

        PlayerShopService.Result result = purchase(f);

        assertEquals(PlayerShopService.Result.FAILED, result);
        assertEquals(96, f.shop.getOffer(0).getTotalAmount());
        assertEquals(1_000L, f.shop.getOffer(0).getPriceCoins());
        verify(f.economy).deposit(f.buyerId, 1_000L, "PLAYER_SHOP_ROLLBACK", "Rollback Shop " + f.shopId);
        verify(f.economy, org.mockito.Mockito.times(2)).persistNow(f.buyerId);
        verify(f.journal).complete(f.transaction);
        verify(f.buyer, never()).saveData();
    }

    private PlayerShopService.Result purchase(Fixture f) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory((InventoryHolder) null, 36)).thenReturn(f.fitInventory);
            PlayerShopService service = new PlayerShopService(f.store, f.economy, f.logging, f.journal);
            return service.purchase(f.buyer, f.shopId, 0);
        }
    }

    private Fixture fixture() {
        Fixture f = new Fixture();
        f.store = mock(PlayerShopStore.class);
        f.economy = mock(EconomyService.class);
        f.logging = mock(LoggingService.class);
        f.journal = mock(PlayerShopPurchaseJournal.class);
        f.buyer = mock(Player.class);
        f.buyerInventory = mock(PlayerInventory.class);
        f.fitInventory = mock(Inventory.class);
        f.buyerId = UUID.randomUUID();
        f.ownerId = UUID.randomUUID();
        f.shopId = UUID.randomUUID();
        f.transaction = UUID.randomUUID();
        f.shop = new PlayerShop(f.shopId, f.ownerId);
        PlayerShopOffer offer = f.shop.getOffer(0);
        offer.setMaterial(Material.DIAMOND);
        offer.setData((short) 0);
        offer.setAmountTop(64);
        offer.setAmountMiddle(32);
        offer.setPriceCoins(1_000L);
        f.shop.setPendingRevenue(100L);

        when(f.store.get(f.shopId)).thenReturn(f.shop);
        when(f.store.saveChecked()).thenReturn(true);
        when(f.buyer.getUniqueId()).thenReturn(f.buyerId);
        when(f.buyer.getInventory()).thenReturn(f.buyerInventory);
        when(f.economy.canDeposit(f.ownerId, 950L)).thenReturn(true);
        when(f.economy.has(f.buyerId, 1_000L)).thenReturn(true);
        when(f.economy.withdraw(f.buyerId, 1_000L, "PLAYER_SHOP", "Kauf Shop " + f.shopId + " Angebot 1")).thenReturn(true);
        when(f.fitInventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<Integer, ItemStack>());
        when(f.buyerInventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<Integer, ItemStack>());
        when(f.journal.begin(f.shopId, 0, f.buyerId, f.ownerId, Material.DIAMOND, (short) 0,
                64, 32, 1_000L, 950L, 100L)).thenReturn(f.transaction);
        return f;
    }

    private static final class Fixture {
        PlayerShopStore store;
        EconomyService economy;
        LoggingService logging;
        PlayerShopPurchaseJournal journal;
        Player buyer;
        PlayerInventory buyerInventory;
        Inventory fitInventory;
        UUID buyerId;
        UUID ownerId;
        UUID shopId;
        UUID transaction;
        PlayerShop shop;
    }
}
