package net.skykings.core.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /speed 1-10 fuer rang-/permissionbasierte Fluggeschwindigkeit. */
public final class SpeedCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.perk.speed";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "Du hast kein Recht fuer /speed.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Verwendung: /speed <1-10|reset>");
            return true;
        }
        if ("reset".equalsIgnoreCase(args[0])) {
            player.setFlySpeed(0.1F);
            player.sendMessage(ChatColor.GREEN + "Fluggeschwindigkeit auf Standard zurueckgesetzt.");
            return true;
        }
        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Bitte nutze eine Zahl von 1 bis 10.");
            return true;
        }
        if (level < 1 || level > 10) {
            player.sendMessage(ChatColor.RED + "Die Fluggeschwindigkeit muss zwischen 1 und 10 liegen.");
            return true;
        }
        player.setFlySpeed(level / 10.0F);
        player.sendMessage(ChatColor.GREEN + "Fluggeschwindigkeit: " + ChatColor.YELLOW + level + "/10");
        return true;
    }
}
