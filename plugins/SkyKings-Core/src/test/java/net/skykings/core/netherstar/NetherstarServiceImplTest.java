package net.skykings.core.netherstar;

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

public class NetherstarServiceImplTest {

    private FakePlayerProfileService profileService;
    private RecordingAuditSink auditSink;
    private NetherstarServiceImpl netherstarService;
    private UUID uuid;

    @Before
    public void setUp() {
        profileService = new FakePlayerProfileService();
        auditSink = new RecordingAuditSink();
        LoggingServiceImpl loggingService = new LoggingServiceImpl(Collections.singletonList(auditSink), Logger.getLogger("test"));
        netherstarService = new NetherstarServiceImpl(profileService, loggingService);

        uuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(uuid, "Tester", Rank.SPIELER, 0L, 10L, 0L, 0L));
    }

    @Test
    public void depositIncreasesBalanceAndLogs() {
        netherstarService.deposit(uuid, 5L, "COMBAT", "PvP-Kill");
        assertEquals(15L, netherstarService.getBalance(uuid));
        assertEquals(1, auditSink.getEvents().size());
        assertEquals(AuditEventType.NETHERSTAR_DEPOSIT, auditSink.getEvents().get(0).getType());
    }

    @Test
    public void withdrawDecreasesBalanceWhenSufficient() {
        assertTrue(netherstarService.withdraw(uuid, 4L, "TEST", null));
        assertEquals(6L, netherstarService.getBalance(uuid));
        assertEquals(AuditEventType.NETHERSTAR_WITHDRAW, auditSink.getEvents().get(0).getType());
    }

    @Test
    public void withdrawFailsWithoutChangingBalanceWhenInsufficient() {
        assertFalse(netherstarService.withdraw(uuid, 999L, "TEST", null));
        assertEquals(10L, netherstarService.getBalance(uuid));
        assertTrue(auditSink.getEvents().isEmpty());
    }

    @Test
    public void setBalanceLogsOldAndNewValue() {
        netherstarService.setBalance(uuid, 50L, "ADMIN", "manual-correction");
        assertEquals(50L, netherstarService.getBalance(uuid));
        assertEquals(AuditEventType.NETHERSTAR_SET, auditSink.getEvents().get(0).getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void depositRejectsNegativeAmount() {
        netherstarService.deposit(uuid, -10L, "TEST", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void depositRejectsZeroAmount() {
        netherstarService.deposit(uuid, 0L, "TEST", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withdrawRejectsNegativeAmount() {
        netherstarService.withdraw(uuid, -5L, "TEST", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setBalanceRejectsNegativeAmount() {
        netherstarService.setBalance(uuid, -1L, "TEST", null);
    }

    @Test
    public void hasReflectsCurrentBalance() {
        assertTrue(netherstarService.has(uuid, 10L));
        assertFalse(netherstarService.has(uuid, 11L));
    }

    @Test(expected = IllegalStateException.class)
    public void operationsOnUnloadedProfileThrow() {
        netherstarService.getBalance(UUID.randomUUID());
    }

    @Test
    public void depositThrowsOnOverflowAndLeavesBalanceAndAuditUnchanged() {
        UUID richUuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(richUuid, "Rich", Rank.SPIELER, 0L, Long.MAX_VALUE - 5, 0L, 0L));

        try {
            netherstarService.deposit(richUuid, 10L, "COMBAT", null);
            org.junit.Assert.fail("Erwartete NetherstarOverflowException");
        } catch (NetherstarOverflowException expected) {
            // erwartet
        }

        assertEquals(Long.MAX_VALUE - 5, netherstarService.getBalance(richUuid));
        assertTrue("Kein Audit-Event fuer eine fehlgeschlagene Transaktion erwartet", auditSink.getEvents().isEmpty());
    }
}
