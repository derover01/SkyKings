package net.skykings.core.command;

import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import net.skykings.core.sound.SoundFeedback;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /speed 1-10 fuer Knight+ oder Spieler mit dauerhaftem Speed-Recht. */
public final class SpeedCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.perk.speed";

    private final RankService rankService;

    public SpeedCommand(RankService rankService) {
        this.rankService = rankService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(PERMISSION)
                && !rankService.hasAtLeast(player.getUniqueId(), Rank.KNIGHT)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens Knight oder das Speed-Recht.");
            SoundFeedback.error(player);
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Verwendung: /speed <1-10|reset>");
            return true;
        }
        if ("reset".equalsIgnoreCase(args[0])) {
            player.setFlySpeed(0.1F);
            player.sendMessage(ChatColor.GREEN + "Fluggeschwindigkeit auf Standard zurueckgesetzt.");
            SoundFeedback.confirm(player);
            return true;
        }
        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Bitte nutze eine Zahl von 1 bis 10.");
            SoundFeedback.error(player);
            return true;
        }
        if (level < 1 || level > 10) {
            player.sendMessage(ChatColor.RED + "Die Fluggeschwindigkeit muss zwischen 1 und 10 liegen.");
            SoundFeedback.error(player);
            return true;
        }
        player.setFlySpeed(level / 10.0F);
        player.sendMessage(ChatColor.GREEN + "Fluggeschwindigkeit: " + ChatColor.YELLOW + level + "/10");
        SoundFeedback.confirm(player);
        return true;
    }
}
