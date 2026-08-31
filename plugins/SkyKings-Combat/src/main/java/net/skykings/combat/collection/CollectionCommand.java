package net.skykings.combat.collection;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /collection oeffnet das Head-Lookbook. */
public final class CollectionCommand implements CommandExecutor {
    private final HeadCollectionService collection;

    public CollectionCommand(HeadCollectionService collection) {
        this.collection = collection;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        int page = 1;
        if (args.length > 0) {
            try { page = Integer.parseInt(args[0]); }
            catch (NumberFormatException ex) {
                player.sendMessage(UiTheme.DANGER + "Ungueltige Seite.");
                SoundFeedback.error(player);
                return true;
            }
        }
        collection.open(player, page);
        return true;
    }
}
