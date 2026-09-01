package net.skykings.combat.retention;

import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Staff-Command zum expliziten Aktivieren/Entfernen des Premium Battle Pass. */
public final class PremiumPassCommand implements CommandExecutor, TabCompleter {
    public static final String PERMISSION = "skykings.admin.battlepass";

    private final BattlePassService battlePass;

    public PremiumPassCommand(BattlePassService battlePass) {
        this.battlePass = battlePass;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(UiTheme.DANGER + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        if (args.length != 2) {
            usage(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (!"give".equals(action) && !"remove".equals(action)) {
            usage(sender);
            return true;
        }

        Player online = Bukkit.getPlayerExact(args[1]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (online == null && !target.hasPlayedBefore())) {
            sender.sendMessage(UiTheme.DANGER + "Spieler nicht gefunden: " + ChatColor.WHITE + args[1]);
            return true;
        }

        boolean enable = "give".equals(action);
        battlePass.setPremium(target.getUniqueId(), enable);
        String name = target.getName() == null ? args[1] : target.getName();

        sender.sendMessage((enable ? UiTheme.SUCCESS : UiTheme.WARNING)
                + "Premium Pass " + (enable ? "vergeben" : "entfernt")
                + UiTheme.MUTED + " • " + UiTheme.TEXT + name);

        if (online != null) {
            online.sendMessage(enable
                    ? UiTheme.LEGENDARY.toString() + ChatColor.BOLD + "PREMIUM PASS FREIGESCHALTET"
                    : UiTheme.WARNING.toString() + ChatColor.BOLD + "PREMIUM PASS ENTFERNT");
            online.sendMessage(UiTheme.MUTED + "Dein Battle-Pass-Status wurde sofort aktualisiert.");
        }
        return true;
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(UiTheme.WARNING + "/premiumpass give <Spieler>");
        sender.sendMessage(UiTheme.WARNING + "/premiumpass remove <Spieler>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) return Collections.emptyList();
        if (args.length == 1) {
            List<String> out = new ArrayList<String>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("give".startsWith(prefix)) out.add("give");
            if ("remove".startsWith(prefix)) out.add("remove");
            return out;
        }
        if (args.length == 2) {
            List<String> out = new ArrayList<String>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(player.getName());
            }
            return out;
        }
        return Collections.emptyList();
    }
}
