package net.skykings.admin.command;

import net.skykings.admin.warp.WarpService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /setwarp und /delwarp teilen denselben Executor. */
public final class WarpAdminCommand implements CommandExecutor, TabCompleter {
    public static final String PERMISSION = "skykings.admin.warps";
    private final WarpService warps;

    public WarpAdminCommand(WarpService warps) {
        this.warps = warps;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(UiTheme.DANGER + "Dafür hast du keine Berechtigung.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(UiTheme.WARNING + "Nutze: /" + label + " <Name>");
            return true;
        }
        if (command.getName().equalsIgnoreCase("setwarp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("/setwarp ist nur ingame verfügbar.");
                return true;
            }
            Player player = (Player) sender;
            warps.set(args[0], player.getLocation());
            player.sendMessage(UiTheme.SUCCESS + "Warp gesetzt: " + ChatColor.WHITE + args[0]);
            SoundFeedback.success(player);
            return true;
        }
        if (!warps.delete(args[0])) {
            sender.sendMessage(UiTheme.DANGER + "Warp nicht gefunden: " + ChatColor.WHITE + args[0]);
            return true;
        }
        sender.sendMessage(UiTheme.SUCCESS + "Warp gelöscht: " + ChatColor.WHITE + args[0]);
        if (sender instanceof Player) SoundFeedback.success((Player) sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION) || args.length != 1 || command.getName().equalsIgnoreCase("setwarp")) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> result = new ArrayList<String>();
        for (String name : warps.names()) if (name.toLowerCase().startsWith(prefix)) result.add(name);
        return result;
    }
}
