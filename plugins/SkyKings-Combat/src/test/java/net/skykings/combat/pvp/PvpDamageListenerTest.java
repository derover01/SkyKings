package net.skykings.combat.pvp;

import net.skykings.combat.newbie.NewbieProtectionService;
import net.skykings.combat.tag.CombatTagService;
import net.skykings.combat.tag.CombatTagServiceImpl;
import net.skykings.combat.tag.LastAttackerService;
import net.skykings.combat.tag.LastAttackerServiceImpl;
import net.skykings.combat.util.MessageCooldownTracker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PvpDamageListenerTest {

    private CombatTagService combatTagService;
    private LastAttackerService lastAttackerService;
    private NewbieProtectionService newbieProtectionService;
    private PvpDamageListener listener;

    private Player attacker;
    private Player victim;
    private UUID attackerUuid;
    private UUID victimUuid;

    @Before
    public void setUp() {
        combatTagService = new CombatTagServiceImpl(15_000L);
        lastAttackerService = new LastAttackerServiceImpl(15_000L);
        newbieProtectionService = mock(NewbieProtectionService.class);
        MessageCooldownTracker feedbackCooldown = new MessageCooldownTracker(2000L);
        listener = new PvpDamageListener(combatTagService, lastAttackerService, newbieProtectionService, feedbackCooldown);

        attackerUuid = UUID.randomUUID();
        victimUuid = UUID.randomUUID();
        attacker = mock(Player.class);
        when(attacker.getUniqueId()).thenReturn(attackerUuid);
        victim = mock(Player.class);
        when(victim.getUniqueId()).thenReturn(victimUuid);
    }

    private EntityDamageByEntityEvent event(Entity damager, Entity target) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(damager);
        when(event.getEntity()).thenReturn(target);
        return event;
    }

    @Test
    public void regularHitTagsBothPlayers() {
        when(newbieProtectionService.isProtected(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        listener.onPvpDamage(event(attacker, victim));

        assertTrue(combatTagService.isTagged(attackerUuid));
        assertTrue(combatTagService.isTagged(victimUuid));
    }

    @Test
    public void regularHitRecordsLastAttackerForCombatLogout() {
        when(newbieProtectionService.isProtected(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        listener.onPvpDamage(event(attacker, victim));

        assertEquals(attackerUuid, lastAttackerService.getLastAttacker(victimUuid));
    }

    @Test
    public void regularHitDisablesFlightForBothPlayers() {
        when(newbieProtectionService.isProtected(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(attacker.isFlying()).thenReturn(true);
        when(attacker.getAllowFlight()).thenReturn(true);
        when(victim.isFlying()).thenReturn(true);
        when(victim.getAllowFlight()).thenReturn(true);

        listener.onPvpDamage(event(attacker, victim));

        verify(attacker).setFlying(false);
        verify(attacker).setAllowFlight(false);
        verify(victim).setFlying(false);
        verify(victim).setAllowFlight(false);
    }

    @Test
    public void nonPlayerVsPlayerDamageIsIgnored() {
        Entity zombie = mock(Zombie.class);
        EntityDamageByEntityEvent event = event(zombie, victim);

        listener.onPvpDamage(event);

        verify(event, never()).setCancelled(anyBoolean());
        assertFalse(combatTagService.isTagged(victimUuid));
    }

    @Test
    public void protectedVictimCannotBeAttackedAndEventIsCancelled() {
        when(newbieProtectionService.isProtected(victimUuid)).thenReturn(true);
        when(newbieProtectionService.isProtected(attackerUuid)).thenReturn(false);

        EntityDamageByEntityEvent event = event(attacker, victim);
        listener.onPvpDamage(event);

        verify(event).setCancelled(true);
        assertFalse("Bei blockiertem Angriff sollte kein Combat Tag gesetzt werden", combatTagService.isTagged(attackerUuid));
    }

    @Test
    public void protectedAttackerAttackingUnprotectedVictimEndsOwnProtectionAndHitLands() {
        when(newbieProtectionService.isProtected(attackerUuid)).thenReturn(true);
        when(newbieProtectionService.isProtected(victimUuid)).thenReturn(false);

        EntityDamageByEntityEvent event = event(attacker, victim);
        listener.onPvpDamage(event);

        verify(newbieProtectionService).disableProtection(attackerUuid);
        verify(event, never()).setCancelled(true);
        assertTrue("Der erste Angriff soll bereits als Combat zaehlen", combatTagService.isTagged(attackerUuid));
    }

    @Test
    public void attackOnSelfIsIgnored() {
        when(attacker.getUniqueId()).thenReturn(attackerUuid);
        EntityDamageByEntityEvent event = event(attacker, attacker);

        listener.onPvpDamage(event);

        assertFalse(combatTagService.isTagged(attackerUuid));
    }
}
