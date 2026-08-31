package net.skykings.admin.command;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.island.IslandAccessService;
import net.skykings.core.plot.PlotAccessService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/** Schneller Runtime-Check fuer Staff nach Deploy/Restart. */
public final class SystemCheckCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skykings.admin.systemcheck")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS SYSTEM CHECK");
        plugin(sender, "SkyKings-Core");
        plugin(sender, "SkyKings-Combat");
        plugin(sender, "SkyKings-Crates");
        plugin(sender, "SkyKings-Admin");
        plugin(sender, "LuckPerms");
        plugin(sender, "Vault");
        sender.sendMessage(status(Bukkit.getServicesManager().load(SkyKingsCoreAPI.class) != null) + " Core API");
        sender.sendMessage(status(Bukkit.getServicesManager().load(IslandAccessService.class) != null) + " Island Access API");
        sender.sendMessage(status(Bukkit.getServicesManager().load(PlotAccessService.class) != null) + " Plot Access API");
        sender.sendMessage(status(Bukkit.getWorld("SkyPvP") != null) + " SkyPvP Produktionswelt");
        sender.sendMessage(status(Bukkit.getWorld("SkyIslands") != null) + " SkyIslands Welt");
        sender.sendMessage(status(Bukkit.getWorld("SkyPlots") != null) + " SkyPlots Welt");
        sender.sendMessage(ChatColor.GRAY + "Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
        return true;
    }

    private void plugin(CommandSender sender, String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        sender.sendMessage(status(plugin != null && plugin.isEnabled()) + " " + name);
    }

    private String status(boolean ok) {
        return ok ? ChatColor.GREEN + "[OK]" + ChatColor.GRAY : ChatColor.RED + "[FEHLT]" + ChatColor.GRAY;
    }
}
