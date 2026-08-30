package net.skykings.admin.command;

import net.skykings.core.permission.VoucherPermission;
import net.skykings.core.permission.VoucherPermissionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** /rechte <Spieler> <Recht> vergibt ausschliesslich whitelisted Voucher-Rechte. */
public final class RightsAdminCommand implements CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "skykings.admin.rechte";
    private final VoucherPermissionService permissionService;

    public RightsAdminCommand(VoucherPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /rechte <Spieler> <Recht>");
            sender.sendMessage(ChatColor.GRAY + "Verfuegbar: " + available());
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Der Spieler muss aktuell online sein.");
            return true;
        }
        VoucherPermission permission = permissionService.find(args[1]);
        if (permission == null) {
            sender.sendMessage(ChatColor.RED + "Dieses Recht ist nicht fuer Gutscheine freigegeben.");
            sender.sendMessage(ChatColor.GRAY + "Verfuegbar: " + available());
            return true;
        }

        VoucherPermissionService.GrantStatus status =
                permissionService.grant(target.getUniqueId(), permission.getId(), sender.getName());
        if (status == VoucherPermissionService.GrantStatus.BRIDGE_UNAVAILABLE) {
            sender.sendMessage(ChatColor.RED + "LuckPerms ist aktuell nicht verfuegbar.");
            return true;
        }
        if (status != VoucherPermissionService.GrantStatus.GRANTED) {
            sender.sendMessage(ChatColor.RED + "Das Recht konnte nicht vergeben werden.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + target.getName() + " hat jetzt dauerhaft das Recht "
                + permission.getDisplayName() + ChatColor.GREEN + ".");
        target.sendMessage(ChatColor.GOLD + "Du hast das dauerhafte Recht " + permission.getDisplayName()
                + ChatColor.GOLD + " erhalten.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(ADMIN_PERMISSION)) return Collections.emptyList();
        if (args.length == 1) {
            List<String> names = new ArrayList<String>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) names.add(player.getName());
            }
            return names;
        }
        if (args.length == 2) {
            List<String> rights = new ArrayList<String>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (VoucherPermission permission : permissionService.getAll()) {
                if (permission.getId().startsWith(prefix)) rights.add(permission.getId());
            }
            return rights;
        }
        return Collections.emptyList();
    }

    private String available() {
        StringBuilder builder = new StringBuilder();
        for (VoucherPermission permission : permissionService.getAll()) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(permission.getId());
        }
        return builder.toString();
    }
}
