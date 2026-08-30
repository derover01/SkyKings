package net.skykings.combat.tag;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

/** Verhindert, dass /fly waehrend eines aktiven Combat-Tags wieder aktiviert wird. */
public final class CombatFlyCommandListener implements Listener {

    private final CombatTagService combatTagService;

    public CombatFlyCommandListener(CombatTagService combatTagService) {
        this.combatTagService = combatTagService;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null) {
            return;
        }
        String root = message.trim().toLowerCase(Locale.ROOT);
        if (!(root.equals("/fly") || root.startsWith("/fly "))) {
            return;
        }
        if (!combatTagService.isTagged(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        long seconds = Math.max(1L, (combatTagService.getRemainingMillis(event.getPlayer().getUniqueId()) + 999L) / 1000L);
        event.getPlayer().sendMessage(ChatColor.RED + "Im Kampf kannst du /fly nicht benutzen. Noch " + seconds + "s Combat-Tag.");
    }
}
