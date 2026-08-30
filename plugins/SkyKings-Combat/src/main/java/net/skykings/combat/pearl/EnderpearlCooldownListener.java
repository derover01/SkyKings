package net.skykings.combat.pearl;

import net.skykings.combat.util.MessageCooldownTracker;
import net.skykings.core.cooldown.CooldownService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Enderpearl-Cooldown (siehe Auftrag Phase 2, Abschnitt 8). Nutzt Core-{@link CooldownService}
 * (ueberlebt Serverneustarts, keine eigene Cooldown-Datenbank in Combat).
 *
 * <p>Bewusst {@link PlayerInteractEvent} statt {@code ProjectileLaunchEvent}: Wird erst DORT
 * gecancelt, ist der Pearl-Wurf technisch teilweise schon verarbeitet und das Item kann bereits
 * konsumiert sein. {@link PlayerInteractEvent#setCancelled(boolean)} verhindert die Interaktion
 * (und damit den Item-Verbrauch) vollstaendig, bevor irgendetwas passiert ist - so bleibt der
 * Pearl beim Spieler, wie gefordert ("Pearl nicht verlieren").
 */
public final class EnderpearlCooldownListener implements Listener {

    public static final String COOLDOWN_KEY = "combat:enderpearl";

    private final CooldownService cooldownService;
    private final long cooldownDurationMillis;
    private final MessageCooldownTracker feedbackCooldown;

    public EnderpearlCooldownListener(CooldownService cooldownService, long cooldownDurationMillis,
                                       MessageCooldownTracker feedbackCooldown) {
        this.cooldownService = cooldownService;
        this.cooldownDurationMillis = cooldownDurationMillis;
        this.feedbackCooldown = feedbackCooldown;
    }

    // Bewusst OHNE ignoreCancelled=true: PlayerInteractEvent#isCancelled() spiegelt fuer
    // RIGHT_CLICK_AIR (kein angeklickter Block) bereits von Bukkit selbst standardmaessig
    // "cancelled" wider (useInteractedBlock() ist ohne Block DENY) - mit ignoreCancelled=true
    // wuerde dieser Listener fuer den haeufigsten Fall (Perle in die Luft werfen) nie aufgerufen.
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_PEARL) {
            return;
        }

        Player player = event.getPlayer();
        long remaining = cooldownService.getRemainingMillis(player.getUniqueId(), COOLDOWN_KEY);
        if (remaining > 0) {
            event.setCancelled(true);
            sendCooldownFeedback(player, remaining);
            return;
        }

        cooldownService.set(player.getUniqueId(), COOLDOWN_KEY, cooldownDurationMillis);
    }

    private void sendCooldownFeedback(Player player, long remainingMillis) {
        if (!feedbackCooldown.shouldSend(player.getUniqueId())) {
            return;
        }
        double remainingSeconds = Math.max(0.1, Math.ceil(remainingMillis / 100.0) / 10.0);
        player.sendMessage(ChatColor.RED + "Enderperle noch " + remainingSeconds + "s im Cooldown.");
    }
}
