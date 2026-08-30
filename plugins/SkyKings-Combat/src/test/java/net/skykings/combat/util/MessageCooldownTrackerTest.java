package net.skykings.combat.util;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MessageCooldownTrackerTest {

    private MessageCooldownTracker tracker;
    private UUID uuid;

    @Before
    public void setUp() {
        tracker = new MessageCooldownTracker(150L);
        uuid = UUID.randomUUID();
    }

    @Test
    public void firstMessageIsAlwaysAllowed() {
        assertTrue(tracker.shouldSend(uuid));
    }

    @Test
    public void secondMessageWithinIntervalIsBlocked() {
        tracker.shouldSend(uuid);
        assertFalse(tracker.shouldSend(uuid));
    }

    @Test
    public void messageAfterIntervalIsAllowedAgain() throws InterruptedException {
        tracker.shouldSend(uuid);
        Thread.sleep(200L);
        assertTrue(tracker.shouldSend(uuid));
    }

    @Test
    public void differentPlayersHaveIndependentCooldowns() {
        UUID other = UUID.randomUUID();
        tracker.shouldSend(uuid);
        assertTrue(tracker.shouldSend(other));
    }
}
