package net.skykings.core.shop.player;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.logging.LoggingService;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerShopRevenueClaimServiceTest {

    @Test
    public void successfulClaimCommitsBalanceBeforeJournalCompletion() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        PlayerShopRevenueClaimJournal journal = mock(PlayerShopRevenueClaimJournal.class);
        Player owner = mock(Player.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID(), tx = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 12_345L);

        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(true);
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(economy.canDeposit(ownerId, 12_345L)).thenReturn(true);
        when(economy.persistNow(ownerId)).thenReturn(true);
        when(journal.begin(shopId, ownerId, 12_345L)).thenReturn(tx);
        when(journal.complete(tx)).thenReturn(true);

        PlayerShopService service = new PlayerShopService(store, economy, logging, null, journal);
        assertEquals(12_345L, service.claimRevenue(owner, shopId));
        assertEquals(0L, shop.getPendingRevenue());

        verify(economy).deposit(ownerId, 12_345L, "PLAYER_SHOP", "Shop-Einnahmen " + shopId);
        verify(economy).persistNow(ownerId);
        verify(journal).complete(tx);
    }

    @Test
    public void failedDurableBalanceCommitLeavesClaimForManualReview() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        PlayerShopRevenueClaimJournal journal = mock(PlayerShopRevenueClaimJournal.class);
        Player owner = mock(Player.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID(), tx = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 50_000L);

        when(store.get(shopId)).thenReturn(shop);
        when(store.saveChecked()).thenReturn(true);
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(economy.canDeposit(ownerId, 50_000L)).thenReturn(true);
        when(economy.persistNow(ownerId)).thenReturn(false);
        when(journal.begin(shopId, ownerId, 50_000L)).thenReturn(tx);

        PlayerShopService service = new PlayerShopService(store, economy, logging, null, journal);
        assertEquals(0L, service.claimRevenue(owner, shopId));

        // Pending Revenue wurde bereits durable aus player-shops.yml entfernt. Weil unklar ist,
        // ob der Coin-Write eventuell doch auf Disk angekommen ist, darf hier niemals automatisch
        // zurueckgebucht oder erneut ausgezahlt werden. Das Journal erzwingt Staff-Review.
        assertEquals(0L, shop.getPendingRevenue());
        verify(economy).deposit(ownerId, 50_000L, "PLAYER_SHOP", "Shop-Einnahmen " + shopId);
        verify(economy).persistNow(ownerId);
        verify(journal).noteFailure(tx, "REVENUE_BALANCE_COMMIT_FAILED_AFTER_PENDING_ZERO");
        verify(journal, never()).complete(tx);
    }

    @Test
    public void journalReservationFailureLeavesPendingRevenueUntouched() {
        PlayerShopStore store = mock(PlayerShopStore.class);
        EconomyService economy = mock(EconomyService.class);
        LoggingService logging = mock(LoggingService.class);
        PlayerShopRevenueClaimJournal journal = mock(PlayerShopRevenueClaimJournal.class);
        Player owner = mock(Player.class);
        UUID ownerId = UUID.randomUUID(), shopId = UUID.randomUUID();
        PlayerShop shop = shop(shopId, ownerId, 7_500L);

        when(store.get(shopId)).thenReturn(shop);
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(economy.canDeposit(ownerId, 7_500L)).thenReturn(true);
        when(journal.begin(shopId, ownerId, 7_500L)).thenReturn(null);

        PlayerShopService service = new PlayerShopService(store, economy, logging, null, journal);
        assertEquals(0L, service.claimRevenue(owner, shopId));
        assertEquals(7_500L, shop.getPendingRevenue());

        verify(store, never()).saveChecked();
        verify(economy, never()).deposit(ownerId, 7_500L, "PLAYER_SHOP", "Shop-Einnahmen " + shopId);
    }

    private PlayerShop shop(UUID shopId, UUID ownerId, long pendingRevenue) {
        PlayerShop shop = new PlayerShop(shopId, ownerId);
        shop.setPendingRevenue(pendingRevenue);
        return shop;
    }
}
