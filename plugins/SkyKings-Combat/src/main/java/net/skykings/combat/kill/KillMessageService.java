package net.skykings.combat.kill;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Baut kurze SkyKings-Kill-Messages für normale PvP-Kills. */
public final class KillMessageService {

    private static final String[] TEMPLATES = new String[] {
            "%killer% hat %victim% aus dem Himmel geschickt.",
            "%victim% konnte %killer% nicht entkommen.",
            "%killer% hat %victim% im SkyPvP zerlegt.",
            "%victim% wurde von %killer% ausgeschaltet.",
            "%killer% hat %victim% vom Thron gestoßen.",
            "%victim% fiel %killer% zum Opfer."
    };

    public String create(Player killer, Player victim) {
        String template = TEMPLATES[ThreadLocalRandom.current().nextInt(TEMPLATES.length)];
        return ChatColor.DARK_GRAY + "[" + ChatColor.RED + "⚔" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GRAY + template
                .replace("%killer%", ChatColor.GOLD + killer.getName() + ChatColor.GRAY)
                .replace("%victim%", ChatColor.WHITE + victim.getName() + ChatColor.GRAY);
    }
}
