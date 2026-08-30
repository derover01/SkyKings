package net.skykings.core.command;

import net.skykings.core.kit.KitDefinition;
import net.skykings.core.kit.KitGrantResult;
import net.skykings.core.kit.KitGrantService;
import net.skykings.core.kit.KitGui;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** /kit oeffnet die GUI; /kit <name> beansprucht weiterhin direkt ein Rank-Kit. */
public final class KitCommand implements CommandExecutor, TabCompleter {

    private final KitGrantService kitGrantService;
    private final KitGui kitGui;

    public KitCommand(KitGrantService kitGrantService, KitGui kitGui) {
        this.kitGrantService = kitGrantService;
        this.kitGui = kitGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            kitGui.open(player);
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Verwendung: /kit [name]");
            return true;
        }

        KitGrantResult result = kitGrantService.grant(player, args[0]);
        switch (result.getStatus()) {
            case SUCCESS:
                player.sendMessage(ChatColor.GREEN + "Kit " + display(result.getKit()) + ChatColor.GREEN + " erhalten.");
                break;
            case NOT_FOUND:
                player.sendMessage(ChatColor.RED + "Dieses Kit existiert nicht. Nutze /kit fuer die Kit-Uebersicht.");
                break;
            case NO_PERMISSION:
                player.sendMessage(ChatColor.RED + "Dein Rang ist fuer dieses Kit nicht hoch genug.");
                break;
            case COOLDOWN:
                player.sendMessage(ChatColor.RED + "Dieses Kit ist noch " + formatDuration(result.getRemainingMillis())
                        + " im Cooldown.");
                break;
            case INVENTORY_FULL:
                player.sendMessage(ChatColor.RED + "Du brauchst mehr freie Inventarplaetze fuer dieses Kit.");
                break;
            case PROFILE_NOT_LOADED:
                player.sendMessage(ChatColor.RED + "Dein Spielerprofil ist noch nicht geladen. Bitte versuche es erneut.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Das Kit konnte nicht vergeben werden.");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (KitDefinition kit : kitGrantService.getAccessibleKits((Player) sender)) {
            if (kit.getId().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(kit.getId());
            }
        }
        return matches;
    }

    private String display(KitDefinition kit) {
        if (kit == null) {
            return "?";
        }
        return ChatColor.translateAlternateColorCodes('&', kit.getDisplayName());
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(1L, (millis + 999L) / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
