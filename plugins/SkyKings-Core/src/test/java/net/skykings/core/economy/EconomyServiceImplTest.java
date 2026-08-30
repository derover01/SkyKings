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

public class EconomyServiceImplTest {

    private RecordingAuditSink auditSink;
    private EconomyServiceImpl economyService;
    private UUID uuid;

    @Before
    public void setUp() {
        FakePlayerProfileService profileService = new FakePlayerProfileService();
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
}
