package net.skykings.core.command;

import net.skykings.core.protection.MapProtectionService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Staff-Toggle fuer kontrolliertes Editieren geschuetzter Spawn-/PvP-Welten. */
public final class BuildModeCommand implements CommandExecutor {

    private final MapProtectionService protectionService;

    public BuildModeCommand(MapProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(MapProtectionService.BYPASS_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        boolean enabled = protectionService.toggle(player);
        player.sendMessage(enabled
                ? ChatColor.GREEN + "BuildMode aktiviert. Du kannst die geschuetzte Map jetzt bearbeiten."
                : ChatColor.YELLOW + "BuildMode deaktiviert. Die Map ist fuer dich wieder geschuetzt.");
        return true;
    }
}
