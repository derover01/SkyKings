package net.skykings.core.command;

import net.skykings.core.model.Rank;
import net.skykings.core.perk.BuildBlocksGui;
import net.skykings.core.rank.RankService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /bloecke fuer Phoenix+; oeffnet die unbegrenzte No-Sell-Buildblock-Auswahl. */
public final class BlocksCommand implements CommandExecutor {

    private final RankService rankService;
    private final BuildBlocksGui gui;

    public BlocksCommand(RankService rankService, BuildBlocksGui gui) {
        this.rankService = rankService;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!rankService.hasAtLeast(player.getUniqueId(), Rank.PHOENIX)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens den Rang Phoenix fuer /bloecke.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
