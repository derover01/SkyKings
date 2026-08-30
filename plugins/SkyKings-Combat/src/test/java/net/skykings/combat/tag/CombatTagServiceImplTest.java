package net.skykings.combat.tag;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatTagServiceImplTest {

    private CombatTagServiceImpl service;
    private UUID a;
    private UUID b;

    @Before
    public void setUp() {
        service = new CombatTagServiceImpl(150L);
        a = UUID.randomUUID();
        b = UUID.randomUUID();
    }

    @Test
    public void freshPlayerIsNotTagged() {
        assertFalse(service.isTagged(a));
        assertTrue(service.getRemainingMillis(a) == 0L);
    }

    @Test
    public void tagBothTagsBothPlayers() {
        service.tagBoth(a, b);
        assertTrue(service.isTagged(a));
        assertTrue(service.isTagged(b));
    }

    @Test
    public void reTaggingExtendsTheTimer() throws InterruptedException {
        service.tag(a);
        long firstRemaining = service.getRemainingMillis(a);
        Thread.sleep(60L);
        service.tag(a); // erneuter Treffer verlaengert wieder auf die volle Dauer

        long secondRemaining = service.getRemainingMillis(a);
        assertTrue("Erneutes Taggen sollte den Timer wieder verlaengern", secondRemaining > firstRemaining - 30L);
    }

    @Test
    public void tagExpiresAfterConfiguredDuration() throws InterruptedException {
        service.tag(a);
        Thread.sleep(200L);

        assertFalse(service.isTagged(a));
        assertTrue(service.getRemainingMillis(a) == 0L);
    }

    @Test
    public void clearRemovesTagImmediately() {
        service.tag(a);
        assertTrue(service.isTagged(a));

        service.clear(a);

        assertFalse(service.isTagged(a));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonPositiveDurationIsRejected() {
        new CombatTagServiceImpl(0L);
    }
}
