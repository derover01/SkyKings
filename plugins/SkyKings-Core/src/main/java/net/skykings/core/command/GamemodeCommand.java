package net.skykings.core.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /gm fuer Teammitglieder mit explizitem Staff-Recht. */
public final class GamemodeCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.staff.gamemode";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dafür hast du keine Rechte.");
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /gm <0|1|2|3> [Spieler]");
            return true;
        }
        GameMode mode = parse(args[0]);
        if (mode == null) {
            sender.sendMessage(ChatColor.RED + "Ungültiger Gamemode. Nutze 0, 1, 2 oder 3.");
            return true;
        }
        Player target;
        if (args.length == 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Spieler ist nicht online.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Von der Konsole musst du einen Spieler angeben.");
            return true;
        }
        target.setGameMode(mode);
        target.sendMessage(ChatColor.GREEN + "Gamemode: " + ChatColor.WHITE + mode.name());
        if (!target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Gamemode von " + target.getName() + " auf " + mode.name() + " gesetzt.");
        }
        return true;
    }

    private GameMode parse(String raw) {
        if ("0".equals(raw) || "survival".equalsIgnoreCase(raw) || "s".equalsIgnoreCase(raw)) return GameMode.SURVIVAL;
        if ("1".equals(raw) || "creative".equalsIgnoreCase(raw) || "c".equalsIgnoreCase(raw)) return GameMode.CREATIVE;
        if ("2".equals(raw) || "adventure".equalsIgnoreCase(raw) || "a".equalsIgnoreCase(raw)) return GameMode.ADVENTURE;
        if ("3".equals(raw) || "spectator".equalsIgnoreCase(raw) || "sp".equalsIgnoreCase(raw)) return GameMode.SPECTATOR;
        return null;
    }
}
