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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CombatKillServiceImplTest {

    private KillstreakServiceImpl killstreakService;
    private AntiFarmServiceImpl antiFarmService;
    private NetherstarRewardDelivery rewardDelivery;
    private LootProtectionService lootProtectionService;
    private CombatKillServiceImpl service;

    private Player killer;
    private Player victim;
    private UUID killerUuid;
    private UUID victimUuid;
    private MockedStatic<Bukkit> bukkitStatic;

    @Before
    public void setUp() {
        bukkitStatic = mockStatic(Bukkit.class);
        PluginManager pluginManager = mock(PluginManager.class);
        bukkitStatic.when(Bukkit::getPluginManager).thenReturn(pluginManager);

        killstreakService = new KillstreakServiceImpl(1, Collections.singletonList(new KillstreakTier(5, 2, 3)));
        antiFarmService = new AntiFarmServiceImpl(5, 6, 0.5);
        rewardDelivery = mock(NetherstarRewardDelivery.class);
        lootProtectionService = mock(LootProtectionService.class);
        service = new CombatKillServiceImpl(killstreakService, antiFarmService, rewardDelivery,
                lootProtectionService, Logger.getLogger("test"));

        killerUuid = UUID.randomUUID();
        victimUuid = UUID.randomUUID();
        killer = mock(Player.class);
        when(killer.getUniqueId()).thenReturn(killerUuid);
        victim = mock(Player.class);
        when(victim.getUniqueId()).thenReturn(victimUuid);
        when(victim.getLocation()).thenReturn(mock(Location.class));
    }

    @After
    public void tearDown() {
        bukkitStatic.close();
    }

    @Test
    public void legitimateKillGivesPhysicalNetherstarRewardAndTracksStreak() {
        service.handleDeath(victim, killer);

        verify(rewardDelivery).give(killer, 1L);
        assertEquals(1, killstreakService.getStreak(killerUuid));
        verify(lootProtectionService).protectDeathDrops(any(Location.class), eq(killerUuid));
    }

    @Test
    public void nonPvpDeathGrantsNoReward() {
        service.handleDeath(victim, null);
        verify(rewardDelivery, never()).give(any(Player.class), anyLong());
        verify(lootProtectionService, never()).protectDeathDrops(any(), any());
    }

    @Test
    public void nonPvpDeathStillResetsVictimStreak() {
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

        verify(rewardDelivery, never()).give(any(Player.class), anyLong());
        assertEquals(0, killstreakService.getStreak(killerUuid));
    }

    @Test
    public void antiFarmReducesRewardOnSixthKillAgainstSameVictim() {
        for (int i = 0; i < 5; i++) service.handleDeath(victim, killer);
        service.handleDeath(victim, killer);

        verify(rewardDelivery, times(5)).give(killer, 1L);
        assertEquals(6, killstreakService.getStreak(killerUuid));
    }

    @Test
    public void antiFarmBlocksRewardEntirelyFromSeventhKillOnward() {
        for (int i = 0; i < 6; i++) service.handleDeath(victim, killer);
        org.mockito.Mockito.clearInvocations(rewardDelivery);

        service.handleDeath(victim, killer);

        verify(rewardDelivery, never()).give(any(Player.class), anyLong());
        assertEquals("Der Kill zählt statistisch trotzdem für die Killstreak",
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

        verify(rewardDelivery, times(10)).give(eq(killer), anyLong());
    }
}
