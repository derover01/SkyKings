package net.skykings.combat.cosmetic;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /killeffect öffnet die kosmetische Kill-Effect-Auswahl. */
public final class KillEffectCommand implements CommandExecutor {

    private final KillEffectGui gui;

    public KillEffectCommand(KillEffectGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfügbar.");
            return true;
        }
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /killeffect");
            return true;
        }
        gui.open((Player) sender);
        return true;
    }
}
