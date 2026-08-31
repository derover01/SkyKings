package net.skykings.core.command;

import net.skykings.core.enderchest.EnderChestService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /ec oeffnet die persistente mehrseitige SkyKings-Enderchest. */
public final class EnderChestCommand implements CommandExecutor {

    private final EnderChestService enderChestService;

    public EnderChestCommand(EnderChestService enderChestService) {
        this.enderChestService = enderChestService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        enderChestService.open((Player) sender);
        return true;
    }
}
