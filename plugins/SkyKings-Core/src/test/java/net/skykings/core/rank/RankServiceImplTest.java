package net.skykings.core.rank;

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

public class RankServiceImplTest {

    private RecordingAuditSink auditSink;
    private RankServiceImpl rankService;
    private UUID uuid;

    @Before
    public void setUp() {
        FakePlayerProfileService profileService = new FakePlayerProfileService();
        auditSink = new RecordingAuditSink();
        LoggingServiceImpl loggingService = new LoggingServiceImpl(Collections.singletonList(auditSink), Logger.getLogger("test"));
        rankService = new RankServiceImpl(profileService, loggingService);

        uuid = UUID.randomUUID();
        profileService.put(new PlayerProfile(uuid, "Tester", Rank.SPIELER, 0L, 0L, 0L, 0L));
    }

    @Test
    public void setRankUpdatesAndLogs() {
        rankService.setRank(uuid, Rank.KNIGHT, "ADMIN");
        assertEquals(Rank.KNIGHT, rankService.getRank(uuid));
        assertEquals(1, auditSink.getEvents().size());
        assertEquals(AuditEventType.RANK_CHANGE, auditSink.getEvents().get(0).getType());
    }

    @Test
    public void settingSameRankDoesNotLogAgain() {
        rankService.setRank(uuid, Rank.SPIELER, "ADMIN");
        assertTrue(auditSink.getEvents().isEmpty());
    }

    @Test
    public void hasAtLeastFollowsHierarchy() {
        rankService.setRank(uuid, Rank.GOLD);
        assertTrue(rankService.hasAtLeast(uuid, Rank.IRON));
        assertTrue(rankService.hasAtLeast(uuid, Rank.GOLD));
        assertFalse(rankService.hasAtLeast(uuid, Rank.EPIC));
        assertFalse(rankService.hasAtLeast(uuid, Rank.KNIGHT));
    }

    @Test(expected = IllegalStateException.class)
    public void operationsOnUnloadedProfileThrow() {
        rankService.getRank(UUID.randomUUID());
    }
}
