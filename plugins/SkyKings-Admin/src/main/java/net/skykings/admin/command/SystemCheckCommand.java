package net.skykings.admin.command;

import net.skykings.combat.event.EventParticipationService;
import net.skykings.combat.tag.CombatTagServiceImpl;
import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.discord.DiscordNotifier;
import net.skykings.core.island.IslandAccessService;
import net.skykings.core.plot.PlotAccessService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

/** Schneller Phase-10 Runtime-Check fuer Staff nach Deploy/Restart. */
public final class SystemCheckCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skykings.admin.systemcheck")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS SYSTEM CHECK");
        sender.sendMessage(ChatColor.DARK_GRAY + "Server " + Bukkit.getVersion() + " | Java " + System.getProperty("java.version"));

        sender.sendMessage(ChatColor.AQUA + "Module & Services");
        plugin(sender, "SkyKings-Core");
        plugin(sender, "SkyKings-Combat");
        plugin(sender, "SkyKings-Crates");
        plugin(sender, "SkyKings-Admin");
        plugin(sender, "LuckPerms");
        plugin(sender, "Vault");
        SkyKingsCoreAPI coreApi = Bukkit.getServicesManager().load(SkyKingsCoreAPI.class);
        check(sender, coreApi != null, "Core API");
        check(sender, coreApi != null && coreApi.getClanService() != null, "Clan Service");
        check(sender, Bukkit.getServicesManager().load(IslandAccessService.class) != null, "Island Access API");
        check(sender, Bukkit.getServicesManager().load(PlotAccessService.class) != null, "Plot Access API");
        check(sender, CombatTagServiceImpl.liveInstance() != null, "CombatTag Live Service");
        check(sender, EventParticipationService.global() != null, "Event Participation Runtime");
        DiscordNotifier discord = Bukkit.getServicesManager().load(DiscordNotifier.class);
        check(sender, discord != null, "Discord Bridge");

        sender.sendMessage(ChatColor.AQUA + "Kritische Commands");
        command(sender, "plot");
        command(sender, "warp");
        command(sender, "battlepass");
        command(sender, "quests");
        command(sender, "kit");
        command(sender, "duel");
        command(sender, "lms");
        command(sender, "clanwar");
        command(sender, "eventarena");

        sender.sendMessage(ChatColor.AQUA + "Welten");
        check(sender, Bukkit.getWorld("SkyPvP") != null, "SkyPvP Produktionswelt");
        check(sender, Bukkit.getWorld("SkyIslands") != null, "SkyIslands Welt");
        check(sender, Bukkit.getWorld("SkyPlots") != null, "SkyPlots Welt");
        optionalWorld(sender, "SkyEvents");
        optionalWorld(sender, "SkyCommunityEvent");

        int eventPlayers = EventParticipationService.global().snapshot().size();
        sender.sendMessage(ChatColor.GRAY + "Aktive Event-Spieler: " + ChatColor.WHITE + eventPlayers);
        if (discord == null || !discord.isEnabled()) {
            sender.sendMessage(ChatColor.YELLOW + "[OPTIONAL]" + ChatColor.GRAY + " Discord deaktiviert/nicht konfiguriert");
        } else {
            sender.sendMessage((discord.isConfigured("events") ? ChatColor.GREEN + "[OK]" : ChatColor.YELLOW + "[OPTIONAL]")
                    + ChatColor.GRAY + " Discord Events Channel");
            sender.sendMessage((discord.isConfigured("audit") ? ChatColor.GREEN + "[OK]" : ChatColor.YELLOW + "[OPTIONAL]")
                    + ChatColor.GRAY + " Discord Audit Channel");
            sender.sendMessage((discord.isConfigured("status") ? ChatColor.GREEN + "[OK]" : ChatColor.YELLOW + "[OPTIONAL]")
                    + ChatColor.GRAY + " Discord Status Channel");
        }
        sender.sendMessage(ChatColor.GRAY + "Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
        return true;
    }

    private void plugin(CommandSender sender, String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        check(sender, plugin != null && plugin.isEnabled(), name);
    }

    private void command(CommandSender sender, String name) {
        PluginCommand command = Bukkit.getPluginCommand(name);
        check(sender, command != null && command.getPlugin().isEnabled(), "/" + name);
    }

    private void optionalWorld(CommandSender sender, String name) {
        boolean loaded = Bukkit.getWorld(name) != null;
        sender.sendMessage((loaded ? ChatColor.GREEN + "[OK]" : ChatColor.YELLOW + "[OPTIONAL]")
                + ChatColor.GRAY + " " + name);
    }

    private void check(CommandSender sender, boolean ok, String name) {
        sender.sendMessage(status(ok) + " " + name);
    }

    private String status(boolean ok) {
        return ok ? ChatColor.GREEN + "[OK]" + ChatColor.GRAY : ChatColor.RED + "[FEHLT]" + ChatColor.GRAY;
    }
}
