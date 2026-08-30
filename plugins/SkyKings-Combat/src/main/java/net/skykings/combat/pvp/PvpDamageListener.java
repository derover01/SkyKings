package net.skykings.combat.pvp;

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

/**
 * Ein Listener fuer alles, was an einer echten Player-vs-Player-Schadensinteraktion haengt
 * (siehe Auftrag Phase 2, Abschnitte 5, 7, 14, 15): Combat Tag, Fly-Sicherheitslogik,
 * Newbie-Protection-Durchsetzung und der zugehoerige Feedback-Spam-Schutz. Reine
 * Event-Wiring-Klasse - die eigentliche Entscheidungslogik liegt in den injizierten Services.
 */
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
        if (victim == null) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        // Der erste freiwillige Angriff eines geschuetzten Newbies beendet dessen eigenen Schutz
        // sofort und PERMANENT - der Hit selbst zaehlt bereits (siehe Auftrag: "bevorzugtes
        // Verhalten"). Das gilt unabhaengig davon, ob der Treffer am Opfer letztlich durchgeht.
        if (newbieProtectionService.isProtected(attacker.getUniqueId())) {
            newbieProtectionService.disableProtection(attacker.getUniqueId());
        }

        // Ein geschuetzter Newbie kann nicht angegriffen werden - das gilt unabhaengig vom
        // Angreifer-Status oben (auch wenn der Angreifer selbst gerade erst entschuetzt wurde).
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

    private Player asPlayer(Entity entity) {
        return entity instanceof Player ? (Player) entity : null;
    }

    /** Erkennt sowohl Nahkampf (Damager ist der Player) als auch Fernkampf (Damager ist ein von einem Player geschossenes Projektil). */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            Object shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        return null;
    }

    private void secureAgainstFlight(Player player) {
        if (player.isFlying()) {
            player.setFlying(false);
        }
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
        }
    }

    private void sendProtectionFeedback(Player attacker) {
        if (!newbieFeedbackCooldown.shouldSend(attacker.getUniqueId())) {
            return;
        }
        attacker.sendMessage(ChatColor.RED + "Dieser Spieler steht noch unter Newbie-Schutz.");
    }
}
