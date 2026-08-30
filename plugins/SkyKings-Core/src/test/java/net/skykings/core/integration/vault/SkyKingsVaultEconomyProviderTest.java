package net.skykings.core.integration.vault;

import net.milkbowl.vault.economy.EconomyResponse;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.economy.EconomyServiceImpl;
import net.skykings.core.logging.LoggingServiceImpl;
import net.skykings.core.logging.RecordingAuditSink;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.profile.FakePlayerProfileService;
import org.bukkit.OfflinePlayer;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testet den Vault-Adapter isoliert gegen eine echte {@link EconomyServiceImpl} (mit einem
 * In-Memory-Profil-Test-Double). {@code OfflinePlayer} wird per Mockito gemockt und der
 * Bukkit-Online-Check per Predicate injiziert, um ohne laufenden Server bzw. statisches
 * Bukkit-Mocking auszukommen.
 */
public class SkyKingsVaultEconomyProviderTest {

    private FakePlayerProfileService profileService;
    private EconomyService economyService;
    private SkyKingsVaultEconomyProvider provider;
    private UUID onlineUuid;
    private UUID offlineUuid;
    private OfflinePlayer onlinePlayer;
    private OfflinePlayer offlinePlayer;

    @Before
    public void setUp() {
        profileService = new FakePlayerProfileService();
        RecordingAuditSink auditSink = new RecordingAuditSink();
        LoggingServiceImpl loggingService = new LoggingServiceImpl(Collections.singletonList(auditSink), Logger.getLogger("test"));
        economyService = new EconomyServiceImpl(profileService, loggingService);

        onlineUuid = UUID.randomUUID();
        offlineUuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(onlineUuid, "Online", Rank.SPIELER, 100L, 0L, 0L, 0L));
        // offlineUuid bewusst NICHT geladen - simuliert einen nicht online Spieler.

        onlinePlayer = mock(OfflinePlayer.class);
        when(onlinePlayer.getUniqueId()).thenReturn(onlineUuid);
        offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getUniqueId()).thenReturn(offlineUuid);

        Predicate<UUID> onlineCheck = uuid -> uuid.equals(onlineUuid);
        provider = new SkyKingsVaultEconomyProvider(economyService, Logger.getLogger("test"), onlineCheck);
    }

    @Test
    public void getBalanceDelegatesToEconomyServiceForOnlinePlayers() {
        assertEquals(100.0d, provider.getBalance(onlinePlayer), 0.0001);
    }

    @Test
    public void getBalanceReturnsZeroForOfflinePlayersWithoutInventingData() {
        assertEquals(0.0d, provider.getBalance(offlinePlayer), 0.0001);
    }

    @Test
    public void hasAccountReflectsOnlineStatus() {
        assertTrue(provider.hasAccount(onlinePlayer));
        assertFalse(provider.hasAccount(offlinePlayer));
    }

    @Test
    public void depositPlayerDelegatesAndReturnsUpdatedBalance() {
        EconomyResponse response = provider.depositPlayer(onlinePlayer, 50.0d);
        assertTrue(response.transactionSuccess());
        assertEquals(150.0d, response.balance, 0.0001);
        assertEquals(150L, economyService.getBalance(onlineUuid));
    }

    @Test
    public void withdrawPlayerFailsCleanlyWhenInsufficientFunds() {
        EconomyResponse response = provider.withdrawPlayer(onlinePlayer, 999.0d);
        assertFalse(response.transactionSuccess());
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
        assertEquals(100L, economyService.getBalance(onlineUuid));
    }

    @Test
    public void withdrawPlayerSucceedsAndDelegates() {
        EconomyResponse response = provider.withdrawPlayer(onlinePlayer, 40.0d);
        assertTrue(response.transactionSuccess());
        assertEquals(60.0d, response.balance, 0.0001);
        assertEquals(60L, economyService.getBalance(onlineUuid));
    }

    @Test
    public void depositPlayerRejectsNegativeAmountWithoutMutatingBalance() {
        EconomyResponse response = provider.depositPlayer(onlinePlayer, -10.0d);
        assertFalse(response.transactionSuccess());
        assertEquals(100L, economyService.getBalance(onlineUuid));
    }

    @Test
    public void depositPlayerForOfflinePlayerFailsCleanlyWithoutInventingData() {
        EconomyResponse response = provider.depositPlayer(offlinePlayer, 10.0d);
        assertFalse(response.transactionSuccess());
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
    }

    @Test
    public void withdrawPlayerForOfflinePlayerFailsCleanlyWithoutInventingData() {
        EconomyResponse response = provider.withdrawPlayer(offlinePlayer, 10.0d);
        assertFalse(response.transactionSuccess());
    }

    @Test
    public void depositPlayerHandlesOverflowAsCleanFailureNotException() {
        UUID richUuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(richUuid, "Rich", Rank.SPIELER, Long.MAX_VALUE - 5, 0L, 0L, 0L));
        OfflinePlayer richPlayer = mock(OfflinePlayer.class);
        when(richPlayer.getUniqueId()).thenReturn(richUuid);

        Predicate<UUID> onlineCheck = uuid -> uuid.equals(onlineUuid) || uuid.equals(richUuid);
        SkyKingsVaultEconomyProvider richProvider =
                new SkyKingsVaultEconomyProvider(economyService, Logger.getLogger("test"), onlineCheck);

        EconomyResponse response = richProvider.depositPlayer(richPlayer, 10.0d);

        assertFalse(response.transactionSuccess());
        assertEquals(EconomyResponse.ResponseType.FAILURE, response.type);
        assertEquals(Long.MAX_VALUE - 5, economyService.getBalance(richUuid));
    }

    @Test
    public void createPlayerAccountReflectsOnlineStatus() {
        assertTrue(provider.createPlayerAccount(onlinePlayer));
        assertFalse(provider.createPlayerAccount(offlinePlayer));
    }

    @Test
    public void bankMethodsReportNotImplemented() {
        assertFalse(provider.hasBankSupport());
        EconomyResponse response = provider.bankBalance("test-bank");
        assertEquals(EconomyResponse.ResponseType.NOT_IMPLEMENTED, response.type);
        assertTrue(provider.getBanks().isEmpty());
    }

    @Test
    public void currencyMetadataIsWholeNumberCoins() {
        assertEquals(0, provider.fractionalDigits());
        assertEquals("Coins", provider.currencyNamePlural());
        assertEquals("Coin", provider.currencyNameSingular());
    }
}
