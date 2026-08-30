package net.skykings.combat.tag;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LastAttackerServiceImplTest {

    private LastAttackerServiceImpl service;
    private UUID victim;
    private UUID attacker;

    @Before
    public void setUp() {
        service = new LastAttackerServiceImpl(150L);
        victim = UUID.randomUUID();
        attacker = UUID.randomUUID();
    }

    @Test
    public void unknownVictimHasNoLastAttacker() {
        assertNull(service.getLastAttacker(victim));
    }

    @Test
    public void recordedAttackIsReturned() {
        service.recordAttack(victim, attacker);
        assertEquals(attacker, service.getLastAttacker(victim));
    }

    @Test
    public void newerAttackOverridesOlderOne() {
        UUID secondAttacker = UUID.randomUUID();
        service.recordAttack(victim, attacker);
        service.recordAttack(victim, secondAttacker);

        assertEquals(secondAttacker, service.getLastAttacker(victim));
    }

    @Test
    public void expiresAfterValidityWindow() throws InterruptedException {
        service.recordAttack(victim, attacker);
        Thread.sleep(200L);

        assertNull(service.getLastAttacker(victim));
    }

    @Test
    public void clearRemovesEntry() {
        service.recordAttack(victim, attacker);
        service.clear(victim);

        assertNull(service.getLastAttacker(victim));
    }
}
