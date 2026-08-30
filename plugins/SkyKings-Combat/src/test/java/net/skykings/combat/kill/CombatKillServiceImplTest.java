package net.skykings.combat.kill;

import net.skykings.combat.antifarm.AntiFarmServiceImpl;
import net.skykings.combat.killstreak.KillstreakServiceImpl;
import net.skykings.combat.killstreak.KillstreakTier;
import net.skykings.combat.loot.LootProtectionService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CombatKillServiceImplTest {

    private KillstreakServiceImpl killstreakService;
    private AntiFarmServiceImpl antiFarmService;
    private FakeNetherstarService netherstarService;
    private LootProtectionService lootProtectionService;
    private CombatKillServiceImpl service;

    private Player killer;
    private Player victim;
    private UUID killerUuid;
    private UUID victimUuid;

    // CombatKillServiceImpl feuert am Ende ein SkyKingsPlayerKillEvent ueber die statische
    // Bukkit-Fassade - ohne laufenden Server muss das fuer diese Tests weggemockt werden.
    private MockedStatic<Bukkit> bukkitStatic;

    @Before
    public void setUp() {
        bukkitStatic = mockStatic(Bukkit.class);
        PluginManager pluginManager = mock(PluginManager.class);
        bukkitStatic.when(Bukkit::getPluginManager).thenReturn(pluginManager);

        killstreakService = new KillstreakServiceImpl(1, Collections.singletonList(new KillstreakTier(5, 2, 3)));
        antiFarmService = new AntiFarmServiceImpl(5, 6, 0.5);
        netherstarService = new FakeNetherstarService();
        lootProtectionService = mock(LootProtectionService.class);
        service = new CombatKillServiceImpl(killstreakService, antiFarmService, netherstarService,
                lootProtectionService, Logger.getLogger("test"));

        killerUuid = UUID.randomUUID();
        victimUuid = UUID.randomUUID();
        killer = mock(Player.class);
        when(killer.getUniqueId()).thenReturn(killerUuid);
        victim = mock(Player.class);
        when(victim.getUniqueId()).thenReturn(victimUuid);
        Location deathLocation = mock(Location.class);
        when(victim.getLocation()).thenReturn(deathLocation);
    }

    @After
    public void tearDown() {
        bukkitStatic.close();
    }

    @Test
    public void legitimateKillGrantsNetherstarRewardAndTracksStreak() {
        service.handleDeath(victim, killer);

        assertEquals(1L, netherstarService.getBalance(killerUuid));
        assertEquals(1, killstreakService.getStreak(killerUuid));
        verify(lootProtectionService).protectDeathDrops(any(Location.class), org.mockito.ArgumentMatchers.eq(killerUuid));
    }

    @Test
    public void nonPvpDeathGrantsNoReward() {
        service.handleDeath(victim, null);

        assertEquals(0L, netherstarService.getBalance(victimUuid));
        assertEquals(0, netherstarService.getDepositCallCount());
        verify(lootProtectionService, never()).protectDeathDrops(any(), any());
    }

    @Test
    public void nonPvpDeathStillResetsVictimStreak() {
        // Simuliert einen vorher aufgebauten Streak des "Opfers" (z. B. es hatte selbst Kills).
        killstreakService.recordKill(victimUuid);
        killstreakService.recordKill(victimUuid);
        assertEquals(2, killstreakService.getStreak(victimUuid));

        service.handleDeath(victim, null);

        assertEquals(0, killstreakService.getStreak(victimUuid));
    }

    @Test
    public void deathAlwaysResetsVictimStreakEvenOnPvpDeath() {
        killstreakService.recordKill(victimUuid);
        assertEquals(1, killstreakService.getStreak(victimUuid));

        service.handleDeath(victim, killer);

        assertEquals(0, killstreakService.getStreak(victimUuid));
    }

    @Test
    public void selfKillIsNeverTreatedAsLegitimateKill() {
        service.handleDeath(killer, killer);

        assertEquals(0L, netherstarService.getBalance(killerUuid));
        assertEquals(0, killstreakService.getStreak(killerUuid));
    }

    @Test
    public void antiFarmReducesRewardOnSixthKillAgainstSameVictim() {
        for (int i = 0; i < 5; i++) {
            service.handleDeath(victim, killer);
        }
        // Kills 1-4: Basis-Tier-Reward je 1. Kill 5: Tier-Reward 2 + Meilenstein-Bonus 3 = 5.
        // Anti-Farm-Multiplikator ist bei Kills 1-5 noch 1.0 (voller Reward).
        long balanceAfterFive = netherstarService.getBalance(killerUuid);
        assertEquals(1 + 1 + 1 + 1 + 5, balanceAfterFive);

        service.handleDeath(victim, killer); // Kill 6 gegen denselben Gegner -> Anti-Farm greift
        long sixthKillReward = netherstarService.getBalance(killerUuid) - balanceAfterFive;

        // Regulaerer Tier-Reward fuer Kill 6 waere 2, mit 0.5x Anti-Farm-Multiplikator gerundet auf 1.
        assertEquals(1L, sixthKillReward);
        assertEquals(6, killstreakService.getStreak(killerUuid));
    }

    @Test
    public void antiFarmBlocksRewardEntirelyFromSeventhKillOnward() {
        for (int i = 0; i < 6; i++) {
            service.handleDeath(victim, killer);
        }
        long balanceAfterSix = netherstarService.getBalance(killerUuid);

        service.handleDeath(victim, killer); // Kill 7 gegen denselben Gegner

        assertEquals("Ab Kill 7 gegen denselben Gegner darf kein weiterer Reward hinzukommen",
                balanceAfterSix, netherstarService.getBalance(killerUuid));
        assertEquals("Der Kill zaehlt statistisch trotzdem fuer die Killstreak",
                7, killstreakService.getStreak(killerUuid));
    }

    @Test
    public void killingDifferentVictimsDoesNotTriggerAntiFarm() {
        for (int i = 0; i < 10; i++) {
            Player freshVictim = mock(Player.class);
            when(freshVictim.getUniqueId()).thenReturn(UUID.randomUUID());
            when(freshVictim.getLocation()).thenReturn(mock(Location.class));
            service.handleDeath(freshVictim, killer);
        }

        assertEquals(10, netherstarService.getDepositCallCount());
    }
}
