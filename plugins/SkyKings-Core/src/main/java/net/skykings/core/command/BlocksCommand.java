package net.skykings.core.command;

import net.skykings.core.model.Rank;
import net.skykings.core.perk.BuildBlocksGui;
import net.skykings.core.rank.RankService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /bloecke fuer Phoenix+ oder Spieler mit einem Block-Gutscheinrecht. */
public final class BlocksCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.perk.blocks";

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
        if (!player.isOp() && !player.hasPermission(PERMISSION)
                && !rankService.hasAtLeast(player.getUniqueId(), Rank.PHOENIX)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens Phoenix oder das Blöcke-Recht.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
