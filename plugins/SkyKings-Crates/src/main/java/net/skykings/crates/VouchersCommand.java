package net.skykings.crates;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Oeffnet das Owner/Admin-GUI zur Gutschein-Erzeugung. */
public final class VouchersCommand implements CommandExecutor {
    private final VoucherAdminGui gui;

    public VouchersCommand(VoucherAdminGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("skykings.admin.vouchers")) {
            player.sendMessage(ChatColor.RED + "Dafuer hast du keine Rechte.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
