package net.skykings.combat.pvp;

import net.skykings.combat.event.EventParticipationService;
import net.skykings.combat.newbie.NewbieProtectionService;
import net.skykings.combat.tag.CombatTagService;
import net.skykings.combat.tag.LastAttackerService;
import net.skykings.combat.util.MessageCooldownTracker;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Zentrale Open-World-PvP-Regeln mit harter Isolation fuer Serverevents. */
public final class PvpDamageListener implements Listener {

    private final CombatTagService combatTagService;
    private final LastAttackerService lastAttackerService;
    private final NewbieProtectionService newbieProtectionService;
    private final MessageCooldownTracker newbieFeedbackCooldown;

    public PvpDamageListener(CombatTagService combatTagService, LastAttackerService lastAttackerService,
                              NewbieProtectionService newbieProtectionService, MessageCooldownTracker newbieFeedbackCooldown) {
        this.combatTagService = combatTagService;
        this.lastAttackerService = lastAttackerService;
        this.newbieProtectionService = newbieProtectionService;
        this.newbieFeedbackCooldown = newbieFeedbackCooldown;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        Player victim = asPlayer(event.getEntity());
        if (victim == null) return;
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;

        EventParticipationService events = EventParticipationService.global();
        boolean attackerEvent = events.isInEvent(attacker.getUniqueId());
        boolean victimEvent = events.isInEvent(victim.getUniqueId());
        if (attackerEvent || victimEvent) {
            // Event-Teilnehmer sind von der Open World getrennt. Nur zwei Spieler derselben
            // Session duerfen sich treffen. Newbie-Schutz, CombatTag und LastAttacker bleiben
            // dabei bewusst unangetastet und werden vom jeweiligen Event-Controller verwaltet.
            if (!events.isSameSession(attacker.getUniqueId(), victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            secureAgainstFlight(attacker);
            secureAgainstFlight(victim);
            return;
        }

        if (newbieProtectionService.isProtected(attacker.getUniqueId())) {
            newbieProtectionService.disableProtection(attacker.getUniqueId());
        }

        if (newbieProtectionService.isProtected(victim.getUniqueId())) {
            event.setCancelled(true);
            sendProtectionFeedback(attacker);
            return;
        }

        combatTagService.tagBoth(attacker.getUniqueId(), victim.getUniqueId());
        lastAttackerService.recordAttack(victim.getUniqueId(), attacker.getUniqueId());
        secureAgainstFlight(attacker);
        secureAgainstFlight(victim);
    }

    private Player asPlayer(Entity entity) { return entity instanceof Player ? (Player) entity : null; }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            Object shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    private void secureAgainstFlight(Player player) {
        if (player.isFlying()) player.setFlying(false);
        if (player.getAllowFlight()) player.setAllowFlight(false);
    }

    private void sendProtectionFeedback(Player attacker) {
        if (!newbieFeedbackCooldown.shouldSend(attacker.getUniqueId())) return;
        attacker.sendMessage(ChatColor.RED + "Dieser Spieler steht noch unter Newbie-Schutz.");
    }
}
