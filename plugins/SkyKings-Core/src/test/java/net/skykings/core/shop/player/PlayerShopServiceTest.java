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

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PlayerShopServiceTest {

    @Test
    public void ownerCannotPurchaseOwnOffer() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player owner = mock(Player.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 0, 16, 0, 1_000L, 0L);
        when(store.get(shopId)).thenReturn(shop);
        when(owner.getUniqueId()).thenReturn(ownerId);

        PlayerShopService service = new PlayerShopService(store, economy, logging);
        assertEquals(PlayerShopService.Result.NOT_ALLOWED, service.purchase(owner, shopId, 0));

        assertEquals(16, shop.getOffer(0).getTotalAmount());
        verify(store, never()).saveChecked();
        verifyNoInteractions(economy);
        verify(owner, never()).getInventory();
    }

    @Test
    public void purchaseSelectedOfferDeliversBothRowsAndAccruesRevenue() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player buyer = mock(Player.class);
        PlayerInventory buyerInventory = mock(PlayerInventory.class);
        Inventory fitInventory = mock(Inventory.class);
        UUID buyerId = UUID.randomUUID(), ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 1, 64, 32, 1_000L, 100L);

        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(true);
        when(buyer.getUniqueId()).thenReturn(buyerId);
        when(buyer.getInventory()).thenReturn(buyerInventory);
        when(economy.canDeposit(ownerId, 950L)).thenReturn(true);
        when(economy.has(buyerId, 1_000L)).thenReturn(true);
        when(economy.withdraw(buyerId, 1_000L, "PLAYER_SHOP", "Kauf Shop " + shopId + " Angebot 2")).thenReturn(true);
        when(fitInventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<Integer, ItemStack>());
        when(buyerInventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<Integer, ItemStack>());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory((InventoryHolder) null, 36)).thenReturn(fitInventory);
            PlayerShopService service = new PlayerShopService(store, economy, logging);
            assertEquals(PlayerShopService.Result.SUCCESS, service.purchase(buyer, shopId, 1));
        }

        assertEquals(0, shop.getOffer(1).getTotalAmount());
        assertEquals(1_050L, shop.getPendingRevenue());
        verify(buyerInventory, org.mockito.Mockito.times(2)).addItem(any(ItemStack.class));
    }

    @Test
    public void partialSecondStackDeliveryRestoresInventoryRefundsAndOffer() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player buyer = mock(Player.class);
        PlayerInventory buyerInventory = mock(PlayerInventory.class);
        Inventory fitInventory = mock(Inventory.class);
        UUID buyerId = UUID.randomUUID(), ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 0, 64, 64, 1_000L, 100L);
        HashMap<Integer, ItemStack> leftovers = new HashMap<Integer, ItemStack>();
        leftovers.put(0, mock(ItemStack.class));

        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(true);
        when(buyer.getUniqueId()).thenReturn(buyerId);
        when(buyer.getInventory()).thenReturn(buyerInventory);
        when(economy.canDeposit(ownerId, 950L)).thenReturn(true);
        when(economy.has(buyerId, 1_000L)).thenReturn(true);
        when(economy.withdraw(buyerId, 1_000L, "PLAYER_SHOP", "Kauf Shop " + shopId + " Angebot 1")).thenReturn(true);
        when(fitInventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<Integer, ItemStack>());
        when(buyerInventory.addItem(any(ItemStack.class)))
                .thenReturn(new HashMap<Integer, ItemStack>())
                .thenReturn(leftovers);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory((InventoryHolder) null, 36)).thenReturn(fitInventory);
            PlayerShopService service = new PlayerShopService(store, economy, logging);
            assertEquals(PlayerShopService.Result.FAILED, service.purchase(buyer, shopId, 0));
        }

        assertEquals(128, shop.getOffer(0).getTotalAmount());
        assertEquals(1_000L, shop.getOffer(0).getPriceCoins());
        assertEquals(100L, shop.getPendingRevenue());
        verify(economy).deposit(buyerId, 1_000L, "PLAYER_SHOP_ROLLBACK", "Rollback Shop " + shopId);
        verify(buyerInventory).setItem(0, null);
    }

    @Test
    public void reservationSaveFailureLeavesOfferAndDoesNotCharge() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player buyer = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        Inventory fitInventory = mock(Inventory.class);
        UUID buyerId = UUID.randomUUID(), ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 3, 20, 0, 500L, 0L);

        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(false);
        when(buyer.getUniqueId()).thenReturn(buyerId);
        when(buyer.getInventory()).thenReturn(inventory);
        when(economy.canDeposit(ownerId, 475L)).thenReturn(true);
        when(economy.has(buyerId, 500L)).thenReturn(true);
        when(fitInventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<Integer, ItemStack>());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory((InventoryHolder) null, 36)).thenReturn(fitInventory);
            PlayerShopService service = new PlayerShopService(store, economy, logging);
            assertEquals(PlayerShopService.Result.FAILED, service.purchase(buyer, shopId, 3));
        }

        assertEquals(20, shop.getOffer(3).getTotalAmount());
        assertEquals(500L, shop.getOffer(3).getPriceCoins());
        verify(economy, never()).withdraw(any(UUID.class), any(Long.class), any(String.class), any(String.class));
        verify(inventory, never()).addItem(any(ItemStack.class));
    }

    @Test
    public void sellerRecoveryOverflowBlocksPurchaseBeforeBuyerMutation() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player buyer = mock(Player.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 0, 16, 0, 1_000L, 0L);
        when(store.get(shopId)).thenReturn(shop);
        when(buyer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(economy.canDeposit(ownerId, 950L)).thenReturn(false);

        PlayerShopService service = new PlayerShopService(store, economy, logging);
        assertEquals(PlayerShopService.Result.FAILED, service.purchase(buyer, shopId, 0));

        assertEquals(16, shop.getOffer(0).getTotalAmount());
        assertEquals(1_000L, shop.getOffer(0).getPriceCoins());
        verify(store, never()).saveChecked();
        verify(buyer, never()).getInventory();
        verify(economy, never()).has(any(UUID.class), any(Long.class));
        verify(economy, never()).withdraw(any(UUID.class), any(Long.class), any(String.class), any(String.class));
    }

    @Test
    public void pendingRevenueOverflowBlocksPurchaseBeforeBuyerChecks() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player buyer = mock(Player.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 0, 1, 0, 1_000L, Long.MAX_VALUE - 500L);
        when(store.get(shopId)).thenReturn(shop);

        PlayerShopService service = new PlayerShopService(store, economy, logging);
        assertEquals(PlayerShopService.Result.FAILED, service.purchase(buyer, shopId, 0));

        assertEquals(1, shop.getOffer(0).getTotalAmount());
        verify(store, never()).saveChecked();
        verifyNoInteractions(economy);
        verify(buyer, never()).getInventory();
    }

    @Test
    public void ownerCanPutAndTakeIndependentOfferRows() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player owner = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        Inventory fitInventory = mock(Inventory.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = new PlayerShop(shopId, ownerId);
        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(true);
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(owner.getInventory()).thenReturn(inventory);
        when(fitInventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<Integer, ItemStack>());

        PlayerShopService service = new PlayerShopService(store, economy, logging);
        assertTrue(service.putOfferStack(owner, shopId, 5, false, plainStack(Material.GOLDEN_APPLE, 64, (short) 0)));
        assertTrue(service.putOfferStack(owner, shopId, 5, true, plainStack(Material.GOLDEN_APPLE, 64, (short) 0)));
        assertTrue(service.setOfferPrice(owner, shopId, 5, 2_000_000L));
        assertEquals(128, shop.getOffer(5).getTotalAmount());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory((InventoryHolder) null, 36)).thenReturn(fitInventory);
            ItemStack returned = service.takeOfferStack(owner, shopId, 5, true);
            assertNotNull(returned);
            assertEquals(64, returned.getAmount());
        }
        assertEquals(64, shop.getOffer(5).getTotalAmount());
        assertEquals(2_000_000L, shop.getOffer(5).getPriceCoins());
    }

    @Test
    public void secondRowMustMatchFirstRowMaterialAndData() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player owner = mock(Player.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = new PlayerShop(shopId, ownerId);
        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(true);
        when(owner.getUniqueId()).thenReturn(ownerId);
        PlayerShopService service = new PlayerShopService(store, economy, logging);

        assertTrue(service.putOfferStack(owner, shopId, 2, false, plainStack(Material.DIAMOND, 32, (short) 0)));
        assertFalse(service.putOfferStack(owner, shopId, 2, true, plainStack(Material.EMERALD, 32, (short) 0)));
        assertEquals(32, shop.getOffer(2).getTotalAmount());
        assertEquals(Material.DIAMOND, shop.getOffer(2).getMaterial());
    }

    @Test
    public void revenueClaimRollsBackWhenStoreSaveFails() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player owner = mock(Player.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 0, 1, 0, 500L, 12_345L);
        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(false);
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(economy.canDeposit(ownerId, 12_345L)).thenReturn(true);

        PlayerShopService service = new PlayerShopService(store, economy, logging);
        assertEquals(0L, service.claimRevenue(owner, shopId));
        assertEquals(12_345L, shop.getPendingRevenue());
        verify(economy, never()).deposit(any(UUID.class), any(Long.class), any(String.class), any(String.class));
    }

    private ItemStack plainStack(Material material, int amount, short data) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.getDurability()).thenReturn(data);
        when(stack.hasItemMeta()).thenReturn(false);
        when(stack.getEnchantments()).thenReturn(Collections.emptyMap());
        return stack;
    }

    private PlayerShop shop(UUID shopId, UUID ownerId, int offerIndex, int top, int middle, long price, long pending) {
        PlayerShop shop = new PlayerShop(shopId, ownerId);
        PlayerShopOffer offer = shop.getOffer(offerIndex);
        offer.setMaterial(Material.DIAMOND);
        offer.setData((short) 0);
        offer.setAmountTop(top);
        offer.setAmountMiddle(middle);
        offer.setPriceCoins(price);
        shop.setPendingRevenue(pending);
        return shop;
    }
}
