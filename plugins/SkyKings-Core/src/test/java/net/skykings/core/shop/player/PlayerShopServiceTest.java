package net.skykings.core.shop.player;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.LoggingService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerShopServiceTest {

    @Test
    public void completedPurchasePaysSellerDirectlyWhenRevenueSaveFails() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player buyer = mock(Player.class);
        PlayerInventory buyerInventory = mock(PlayerInventory.class);
        Inventory fitInventory = mock(Inventory.class);

        UUID buyerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        PlayerShop shop = configuredShop(shopId, ownerId, 10, 2, 1_000L, 100L);

        when(store.get(shopId)).thenReturn(shop);
        // 1: Stock-Reservierung erfolgreich, 2: neues Pending-Revenue kann nicht persistiert werden.
        when(store.saveChecked()).thenReturn(true, false);
        when(buyer.getUniqueId()).thenReturn(buyerId);
        when(buyer.getInventory()).thenReturn(buyerInventory);
        when(economy.has(buyerId, 1_000L)).thenReturn(true);
        when(economy.withdraw(buyerId, 1_000L, "PLAYER_SHOP", "Kauf Shop " + shopId)).thenReturn(true);
        when(fitInventory.addItem(any())).thenReturn(new HashMap<Integer, org.bukkit.inventory.ItemStack>());
        when(buyerInventory.addItem(any())).thenReturn(new HashMap<Integer, org.bukkit.inventory.ItemStack>());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory((InventoryHolder) null, 36)).thenReturn(fitInventory);
            PlayerShopService service = new PlayerShopService(store, economy, logging);

            assertEquals(PlayerShopService.Result.SUCCESS, service.purchase(buyer, shopId));
        }

        assertEquals(8, shop.getStock());
        // Fehlgeschlagener Save darf den unsicheren In-Memory-Wert nicht spaeter erneut persistieren.
        assertEquals(100L, shop.getPendingRevenue());
        verify(economy).deposit(ownerId, 950L, "PLAYER_SHOP_RECOVERY",
                "Direktauszahlung nach Revenue-Save-Fehler " + shopId);
    }

    @Test
    public void revenueClaimDoesNotPayWhenReservationSaveFails() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player owner = mock(Player.class);

        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        PlayerShop shop = configuredShop(shopId, ownerId, 5, 1, 500L, 12_345L);

        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(false);
        when(owner.getUniqueId()).thenReturn(ownerId);

        PlayerShopService service = new PlayerShopService(store, economy, logging);
        assertEquals(0L, service.claimRevenue(owner, shopId));
        assertEquals(12_345L, shop.getPendingRevenue());
        verify(economy, never()).deposit(eq(ownerId), any(Long.class), any(String.class), any(String.class));
    }

    @Test
    public void stockWithdrawDoesNotDeliverWhenReservationSaveFails() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        Player owner = mock(Player.class);
        PlayerInventory ownerInventory = mock(PlayerInventory.class);
        Inventory fitInventory = mock(Inventory.class);

        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        PlayerShop shop = configuredShop(shopId, ownerId, 10, 1, 500L, 0L);

        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(false);
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(owner.getInventory()).thenReturn(ownerInventory);
        when(fitInventory.addItem(any())).thenReturn(new HashMap<Integer, org.bukkit.inventory.ItemStack>());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory((InventoryHolder) null, 36)).thenReturn(fitInventory);
            PlayerShopService service = new PlayerShopService(store, economy, logging);

            assertFalse(service.withdrawStock(owner, shopId, 4));
        }

        assertEquals(10, shop.getStock());
        verify(ownerInventory, never()).addItem(any());
    }

    private PlayerShop configuredShop(UUID shopId, UUID ownerId, int stock, int amountPerSale,
                                      long price, long pendingRevenue) {
        PlayerShop shop = new PlayerShop(shopId, ownerId);
        shop.setMaterial(Material.DIAMOND);
        shop.setData((short) 0);
        shop.setStock(stock);
        shop.setAmountPerSale(amountPerSale);
        shop.setPriceCoins(price);
        shop.setPendingRevenue(pendingRevenue);
        return shop;
    }
}
