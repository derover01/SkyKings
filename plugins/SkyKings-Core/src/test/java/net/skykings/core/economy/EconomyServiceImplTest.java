package net.skykings.core.economy;

import net.skykings.core.logging.AuditEventType;
import net.skykings.core.logging.LoggingServiceImpl;
import net.skykings.core.logging.RecordingAuditSink;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.profile.FakePlayerProfileService;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EconomyServiceImplTest {

    private FakePlayerProfileService profileService;
    private RecordingAuditSink auditSink;
    private EconomyServiceImpl economyService;
    private UUID uuid;

    @Before
    public void setUp() {
        profileService = new FakePlayerProfileService();
        auditSink = new RecordingAuditSink();
        LoggingServiceImpl loggingService = new LoggingServiceImpl(Collections.singletonList(auditSink), Logger.getLogger("test"));
        economyService = new EconomyServiceImpl(profileService, loggingService);

        uuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(uuid, "Tester", Rank.SPIELER, 100L, 0L, 0L, 0L));
    }

    @Test
    public void depositIncreasesBalanceAndLogs() {
        economyService.deposit(uuid, 50L, "TEST", "unit-test");
        assertEquals(150L, economyService.getBalance(uuid));
        assertEquals(1, auditSink.getEvents().size());
        assertEquals(AuditEventType.ECONOMY_DEPOSIT, auditSink.getEvents().get(0).getType());
    }

    @Test
    public void depositCanCreditExistingPersistedProfileAfterLogout() {
        final UUID offlineUuid = UUID.randomUUID();
        final PlayerProfile persisted = new PlayerProfile(offlineUuid, "OfflineWinner", Rank.SPIELER, 250L, 0L, 0L, 0L);
        FakePlayerProfileService persistedService = new FakePlayerProfileService() {
            @Override
            public PlayerProfile loadExisting(UUID target) {
                if (!offlineUuid.equals(target)) return null;
                put(persisted);
                return persisted;
            }
        };
        RecordingAuditSink sink = new RecordingAuditSink();
        EconomyServiceImpl service = new EconomyServiceImpl(persistedService,
                new LoggingServiceImpl(Collections.singletonList(sink), Logger.getLogger("offline-credit-test")));

        service.deposit(offlineUuid, 750L, "JACKPOT", "offline winner");

        assertEquals(1_000L, service.getBalance(offlineUuid));
        assertEquals(1, sink.getEvents().size());
        assertEquals(AuditEventType.ECONOMY_DEPOSIT, sink.getEvents().get(0).getType());
    }

    @Test
    public void depositDoesNotCreateUnknownProfile() {
        UUID unknown = UUID.randomUUID();
        try {
            economyService.deposit(unknown, 100L, "TEST", "unknown");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertTrue(auditSink.getEvents().isEmpty());
    }

    @Test
    public void withdrawDecreasesBalanceWhenSufficient() {
        assertTrue(economyService.withdraw(uuid, 40L, "TEST", null));
        assertEquals(60L, economyService.getBalance(uuid));
    }

    @Test
    public void withdrawFailsWithoutChangingBalanceWhenInsufficient() {
        assertFalse(economyService.withdraw(uuid, 999L, "TEST", null));
        assertEquals(100L, economyService.getBalance(uuid));
        assertTrue(auditSink.getEvents().isEmpty());
    }

    @Test
    public void setBalanceLogsOldAndNewValue() {
        economyService.setBalance(uuid, 500L, "ADMIN", "manual-correction");
        assertEquals(500L, economyService.getBalance(uuid));
        assertEquals(AuditEventType.ECONOMY_SET, auditSink.getEvents().get(0).getType());
    }

    @Test
    public void setBalanceCanUpdateExistingPersistedProfileAfterLogout() {
        final UUID offlineUuid = UUID.randomUUID();
        final PlayerProfile persisted = new PlayerProfile(offlineUuid, "OfflineAdminTarget", Rank.SPIELER, 250L, 0L, 0L, 0L);
        FakePlayerProfileService persistedService = new FakePlayerProfileService() {
            @Override
            public PlayerProfile loadExisting(UUID target) {
                if (!offlineUuid.equals(target)) return null;
                put(persisted);
                return persisted;
            }
        };
        RecordingAuditSink sink = new RecordingAuditSink();
        EconomyServiceImpl service = new EconomyServiceImpl(persistedService,
                new LoggingServiceImpl(Collections.singletonList(sink), Logger.getLogger("offline-set-test")));

        service.setBalance(offlineUuid, 5_000L, "ADMIN", "manual offline correction");

        assertEquals(5_000L, service.getBalance(offlineUuid));
        assertEquals(1, sink.getEvents().size());
        assertEquals(AuditEventType.ECONOMY_SET, sink.getEvents().get(0).getType());
    }

    @Test
    public void setBalanceDoesNotCreateUnknownProfile() {
        UUID unknown = UUID.randomUUID();
        try {
            economyService.setBalance(unknown, 500L, "ADMIN", "unknown");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertTrue(auditSink.getEvents().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void depositRejectsNegativeAmount() {
        economyService.deposit(uuid, -10L, "TEST", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void depositRejectsZeroAmount() {
        economyService.deposit(uuid, 0L, "TEST", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withdrawRejectsNegativeAmount() {
        economyService.withdraw(uuid, -5L, "TEST", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setBalanceRejectsNegativeAmount() {
        economyService.setBalance(uuid, -1L, "TEST", null);
    }

    @Test
    public void hasReflectsCurrentBalance() {
        assertTrue(economyService.has(uuid, 100L));
        assertFalse(economyService.has(uuid, 101L));
    }

    @Test(expected = IllegalStateException.class)
    public void operationsOnUnloadedProfileThrow() {
        economyService.getBalance(UUID.randomUUID());
    }

    @Test
    public void depositThrowsOnOverflowAndLeavesBalanceAndAuditUnchanged() {
        UUID richUuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(richUuid, "Rich", Rank.SPIELER, Long.MAX_VALUE - 5, 0L, 0L, 0L));

        try {
            economyService.deposit(richUuid, 10L, "TEST", null);
            fail("Erwartete EconomyOverflowException");
        } catch (EconomyOverflowException expected) {
            // erwartet
        }

        assertEquals(Long.MAX_VALUE - 5, economyService.getBalance(richUuid));
        assertTrue("Kein Audit-Event fuer eine fehlgeschlagene Transaktion erwartet", auditSink.getEvents().isEmpty());
    }

    @Test
    public void depositRightAtOverflowBoundarySucceeds() {
        UUID boundaryUuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(boundaryUuid, "Boundary", Rank.SPIELER, Long.MAX_VALUE - 10, 0L, 0L, 0L));

        economyService.deposit(boundaryUuid, 10L, "TEST", null);

        assertEquals(Long.MAX_VALUE, economyService.getBalance(boundaryUuid));
    }
}
