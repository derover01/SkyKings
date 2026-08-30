package net.skykings.core.rank;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.model.Rank;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RankProgressionServiceTest {

    private UUID uuid;
    private RankService rankService;
    private EconomyService economyService;
    private RankProgressionConfig config;
    private RankProgressionService service;

    @Before
    public void setUp() {
        uuid = UUID.randomUUID();
        rankService = mock(RankService.class);
        economyService = mock(EconomyService.class);
        config = mock(RankProgressionConfig.class);
        service = new RankProgressionService(rankService, economyService, config);
    }

    @Test
    public void buysExactlyNextFreeRank() {
        when(rankService.getRank(uuid)).thenReturn(Rank.SPIELER);
        when(config.getCost(Rank.IRON)).thenReturn(150000L);
        when(economyService.has(uuid, 150000L)).thenReturn(true);
        when(economyService.withdraw(uuid, 150000L, "RANKUP", "Free-Rank-Kauf zu IRON")).thenReturn(true);

        RankProgressionResult result = service.purchaseNext(uuid);

        assertEquals(RankProgressionResult.Status.SUCCESS, result.getStatus());
        assertEquals(Rank.IRON, result.getTargetRank());
        verify(rankService).setRank(uuid, Rank.IRON, "RANKUP");
    }

    @Test
    public void insufficientCoinsNeverChangesRank() {
        when(rankService.getRank(uuid)).thenReturn(Rank.GOLD);
        when(config.getCost(Rank.EPIC)).thenReturn(2500000L);
        when(economyService.has(uuid, 2500000L)).thenReturn(false);

        RankProgressionResult result = service.purchaseNext(uuid);

        assertEquals(RankProgressionResult.Status.INSUFFICIENT_COINS, result.getStatus());
        verify(economyService, never()).withdraw(org.mockito.ArgumentMatchers.eq(uuid), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
        verify(rankService, never()).setRank(org.mockito.ArgumentMatchers.eq(uuid), org.mockito.ArgumentMatchers.any(Rank.class), anyString());
    }

    @Test
    public void diamondCannotBuyPaidRank() {
        when(rankService.getRank(uuid)).thenReturn(Rank.DIAMOND);

        RankProgressionResult result = service.purchaseNext(uuid);

        assertEquals(RankProgressionResult.Status.MAX_FREE_RANK, result.getStatus());
        assertNull(result.getTargetRank());
        verify(economyService, never()).withdraw(org.mockito.ArgumentMatchers.eq(uuid), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
    }

    @Test
    public void paidRankCannotUseRankup() {
        when(rankService.getRank(uuid)).thenReturn(Rank.KNIGHT);

        RankProgressionResult result = service.purchaseNext(uuid);

        assertEquals(RankProgressionResult.Status.PAID_RANK, result.getStatus());
        verify(economyService, never()).withdraw(org.mockito.ArgumentMatchers.eq(uuid), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
    }

    @Test
    public void refundsCoinsWhenRankChangeFails() {
        when(rankService.getRank(uuid)).thenReturn(Rank.SPIELER);
        when(config.getCost(Rank.IRON)).thenReturn(150000L);
        when(economyService.has(uuid, 150000L)).thenReturn(true);
        when(economyService.withdraw(uuid, 150000L, "RANKUP", "Free-Rank-Kauf zu IRON")).thenReturn(true);
        doThrow(new RuntimeException("rank save failed")).when(rankService).setRank(uuid, Rank.IRON, "RANKUP");

        assertThrows(RuntimeException.class, () -> service.purchaseNext(uuid));
        verify(economyService).deposit(uuid, 150000L, "RANKUP_REFUND", "Rueckerstattung nach fehlgeschlagenem Rankup");
    }
}
