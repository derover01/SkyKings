package net.skykings.combat.killstreak;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class KillstreakServiceImplTest {

    private KillstreakServiceImpl service;
    private UUID killer;

    @Before
    public void setUp() {
        service = new KillstreakServiceImpl(1, Arrays.asList(
                new KillstreakTier(5, 2, 3),
                new KillstreakTier(10, 2, 8),
                new KillstreakTier(20, 3, 15),
                new KillstreakTier(30, 3, 25),
                new KillstreakTier(50, 4, 50),
                new KillstreakTier(100, 5, 125)
        ));
        killer = UUID.randomUUID();
    }

    @Test
    public void streaksOneToFourUseBaseRewardWithoutBonus() {
        for (int i = 1; i <= 4; i++) {
            KillstreakResult result = service.recordKill(killer);
            assertEquals(i, result.getNewStreak());
            assertEquals(1L, result.getPerKillReward());
            assertEquals(0L, result.getMilestoneBonus());
            assertEquals(1L, result.getTotalReward());
        }
    }

    @Test
    public void streakFiveGrantsPerKillAndMilestoneBonusExactlyOnce() {
        recordKills(4);
        KillstreakResult fifth = service.recordKill(killer);
        assertEquals(5, fifth.getNewStreak());
        assertEquals(2L, fifth.getPerKillReward());
        assertEquals(3L, fifth.getMilestoneBonus());
        assertEquals(5L, fifth.getTotalReward());

        KillstreakResult sixth = service.recordKill(killer);
        assertEquals(2L, sixth.getPerKillReward());
        assertEquals(0L, sixth.getMilestoneBonus());
    }

    @Test
    public void streakTenGrantsSamePerKillAsFiveButDifferentBonus() {
        recordKills(9);
        KillstreakResult tenth = service.recordKill(killer);
        assertEquals(10, tenth.getNewStreak());
        assertEquals(2L, tenth.getPerKillReward());
        assertEquals(8L, tenth.getMilestoneBonus());
    }

    @Test
    public void streakTwentyThirtyFiftyHundredMatchConfiguredTiers() {
        recordKills(19);
        assertMilestone(service.recordKill(killer), 20, 3L, 15L);

        recordKills(9); // -> Streak 30
        assertMilestone(service.recordKill(killer), 30, 3L, 25L);

        recordKills(19); // -> Streak 50
        assertMilestone(service.recordKill(killer), 50, 4L, 50L);

        recordKills(49); // -> Streak 100
        assertMilestone(service.recordKill(killer), 100, 5L, 125L);
    }

    @Test
    public void deathResetsStreakToZero() {
        recordKills(7);
        assertEquals(7, service.getStreak(killer));

        service.reset(killer);

        assertEquals(0, service.getStreak(killer));
        KillstreakResult afterReset = service.recordKill(killer);
        assertEquals(1, afterReset.getNewStreak());
        assertEquals(1L, afterReset.getPerKillReward());
    }

    @Test
    public void getStreakForUnknownPlayerIsZero() {
        assertEquals(0, service.getStreak(UUID.randomUUID()));
    }

    private void recordKills(int count) {
        for (int i = 0; i < count; i++) {
            service.recordKill(killer);
        }
    }

    private void assertMilestone(KillstreakResult result, int expectedStreak, long expectedPerKill, long expectedBonus) {
        assertEquals(expectedStreak, result.getNewStreak());
        assertEquals(expectedPerKill, result.getPerKillReward());
        assertEquals(expectedBonus, result.getMilestoneBonus());
    }
}
