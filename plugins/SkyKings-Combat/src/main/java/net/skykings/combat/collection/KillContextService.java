package net.skykings.combat.collection;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Merkt kurz vor dem zentralen Kill-Event eine kompakte Killart fuer History/Collection. */
public final class KillContextService implements Listener {
    private final Map<UUID, String> pending = new ConcurrentHashMap<UUID, String>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        String type = "PvP";
        if (victim.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) victim.getLastDamageCause()).getDamager();
            if (damager instanceof Arrow) type = "Bogen";
            else if (damager instanceof Projectile) type = "Projektil";
            else if (damager instanceof Player) type = "Nahkampf";
        } else if (victim.getLastDamageCause() != null) {
            switch (victim.getLastDamageCause().getCause()) {
                case VOID: type = "Void"; break;
                case FALL: type = "Sturz"; break;
                case FIRE:
                case FIRE_TICK:
                case LAVA: type = "Feuer"; break;
                default: type = "PvP"; break;
            }
        }
        pending.put(victim.getUniqueId(), type);
    }

    public String consume(UUID victim) {
        String value = pending.remove(victim);
        return value == null ? "PvP" : value;
    }
}
