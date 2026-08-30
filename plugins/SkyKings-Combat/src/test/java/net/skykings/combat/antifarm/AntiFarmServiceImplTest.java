package net.skykings.combat.antifarm;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class AntiFarmServiceImplTest {

    private AntiFarmServiceImpl service;
    private UUID killer;
    private UUID victim;
    private UUID otherVictim;

    @Before
    public void setUp() {
        service = new AntiFarmServiceImpl(5, 6, 0.5);
        killer = UUID.randomUUID();
        victim = UUID.randomUUID();
        otherVictim = UUID.randomUUID();
    }

    @Test
    public void killsOneToFiveGrantFullReward() {
        for (int i = 1; i <= 5; i++) {
            assertEquals("Kill " + i + " sollte vollen Reward geben",
                    1.0, service.registerKillAndGetMultiplier(killer, victim), 0.0001);
        }
    }

    @Test
    public void killSixGrantsHalfReward() {
        registerKills(killer, victim, 5);
        assertEquals(0.5, service.registerKillAndGetMultiplier(killer, victim), 0.0001);
    }

    @Test
    public void killSevenAndBeyondGrantNoReward() {
        registerKills(killer, victim, 6);
        assertEquals(0.0, service.registerKillAndGetMultiplier(killer, victim), 0.0001);
        assertEquals(0.0, service.registerKillAndGetMultiplier(killer, victim), 0.0001);
    }

    @Test
    public void killingADifferentVictimResetsTheCounter() {
        registerKills(killer, victim, 6); // -> auf 0 Reward runter

        double multiplierForNewVictim = service.registerKillAndGetMultiplier(killer, otherVictim);

        assertEquals("Ein neues Opfer sollte den Zaehler zuruecksetzen", 1.0, multiplierForNewVictim, 0.0001);
    }

    @Test
    public void switchingBackToOriginalVictimStartsFreshCounterToo() {
        registerKills(killer, victim, 6);
        service.registerKillAndGetMultiplier(killer, otherVictim); // Zaehler fuer victim wird verworfen

        double multiplierBackOnOriginalVictim = service.registerKillAndGetMultiplier(killer, victim);

        assertEquals(1.0, multiplierBackOnOriginalVictim, 0.0001);
    }

    @Test
    public void clearResetsStateForKiller() {
        registerKills(killer, victim, 6);
        service.clear(killer);

        assertEquals(1.0, service.registerKillAndGetMultiplier(killer, victim), 0.0001);
    }

    private void registerKills(UUID killerUuid, UUID victimUuid, int count) {
        for (int i = 0; i < count; i++) {
            service.registerKillAndGetMultiplier(killerUuid, victimUuid);
        }
    }
}
