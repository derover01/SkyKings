package net.skykings.combat.kill;

import net.skykings.combat.tag.CombatTagServiceImpl;
import net.skykings.combat.tag.LastAttackerServiceImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CombatDeathListenerTest {

    private CombatKillService combatKillService;
    private CombatTagServiceImpl combatTagService;
    private LastAttackerServiceImpl lastAttackerService;
    private CombatDeathListener listener;

    private Player victim;
    private Player attacker;
    private UUID victimUuid;
    private UUID attackerUuid;

    @Before
    public void setUp() {
        combatKillService = mock(CombatKillService.class);
        combatTagService = new CombatTagServiceImpl(15_000L);
        lastAttackerService = new LastAttackerServiceImpl(15_000L);

        victimUuid = UUID.randomUUID();
        attackerUuid = UUID.randomUUID();
        victim = mock(Player.class);
        when(victim.getUniqueId()).thenReturn(victimUuid);
        attacker = mock(Player.class);
        when(attacker.getUniqueId()).thenReturn(attackerUuid);

        java.util.function.Function<UUID, Player> playerResolver =
                uuid -> uuid.equals(attackerUuid) ? attacker : (uuid.equals(victimUuid) ? victim : null);
        listener = new CombatDeathListener(combatKillService, combatTagService, lastAttackerService,
                Logger.getLogger("test"), playerResolver);
    }

    private PlayerDeathEvent deathEvent(Player victim) {
        return new PlayerDeathEvent(victim, Collections.emptyList(), 0, "died");
    }

    @Test
    public void normalDeathUsesBukkitsOwnKillerResolution() {
        when(victim.getKiller()).thenReturn(attacker);
        listener.onDeath(deathEvent(victim));
        verify(combatKillService).handleDeath(victim, attacker);
    }

    @Test
    public void normalDeathWithoutKillerPassesNullKiller() {
        when(victim.getKiller()).thenReturn(null);
        listener.onDeath(deathEvent(victim));
        verify(combatKillService).handleDeath(victim, null);
    }

    @Test
    public void combatLogoutUsesLastKnownAttackerInsteadOfGetKiller() {
        lastAttackerService.recordAttack(victimUuid, attackerUuid);
        combatTagService.tag(victimUuid);
        when(victim.isDead()).thenReturn(false);

        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);
        when(quitEvent.getPlayer()).thenReturn(victim);
        listener.onQuit(quitEvent);

        verify(victim).setHealth(0.0);
        listener.onDeath(deathEvent(victim));

        verify(combatKillService).handleDeath(victim, attacker);
        verify(victim, never()).getKiller();
    }

    @Test
    public void combatLogoutWithoutKnownAttackerNeverInventsAKiller() {
        combatTagService.tag(victimUuid);
        when(victim.isDead()).thenReturn(false);

        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);
        when(quitEvent.getPlayer()).thenReturn(victim);
        listener.onQuit(quitEvent);
        listener.onDeath(deathEvent(victim));

        verify(combatKillService).handleDeath(victim, null);
        verify(victim, never()).getKiller();
    }

    @Test
    public void quitWithoutActiveCombatTagDoesNotForceDeath() {
        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);
        when(quitEvent.getPlayer()).thenReturn(victim);
        listener.onQuit(quitEvent);
        verify(victim, never()).setHealth(org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    public void quitWhileAlreadyDeadDoesNotTriggerDoubleProcessing() {
        combatTagService.tag(victimUuid);
        when(victim.isDead()).thenReturn(true);

        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);
        when(quitEvent.getPlayer()).thenReturn(victim);
        listener.onQuit(quitEvent);
        verify(victim, never()).setHealth(org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    public void deathClearsVictimStateButKeepsKillerCombatTagged() {
        combatTagService.tagBoth(victimUuid, attackerUuid);
        lastAttackerService.recordAttack(victimUuid, attackerUuid);
        when(victim.getKiller()).thenReturn(attacker);

        listener.onDeath(deathEvent(victim));

        org.junit.Assert.assertFalse(combatTagService.isTagged(victimUuid));
        assertTrue("Killer muss nach dem Kill bis zum regulaeren Ablauf weiter im Combat bleiben",
                combatTagService.isTagged(attackerUuid));
        assertNull(lastAttackerService.getLastAttacker(victimUuid));
    }

    @Test
    public void pendingCombatLogoutStateIsConsumedOnlyOnce() {
        lastAttackerService.recordAttack(victimUuid, attackerUuid);
        combatTagService.tag(victimUuid);
        when(victim.isDead()).thenReturn(false);

        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);
        when(quitEvent.getPlayer()).thenReturn(victim);
        listener.onQuit(quitEvent);

        listener.onDeath(deathEvent(victim));
        listener.onDeath(deathEvent(victim));

        verify(combatKillService, times(1)).handleDeath(victim, attacker);
        verify(combatKillService, times(1)).handleDeath(eq(victim), isNull());
    }
}
