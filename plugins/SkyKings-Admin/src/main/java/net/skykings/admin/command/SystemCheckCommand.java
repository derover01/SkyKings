package net.skykings.admin.command;

import net.skykings.combat.event.EventParticipationService;
import net.skykings.combat.event.EventReturnRecoveryService;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

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
        check(sender, coreApi != null && coreApi.getEconomyService() != null, "Economy Service");
        check(sender, coreApi != null && coreApi.getNetherstarService() != null, "Netherstar Service");
        check(sender, coreApi != null && coreApi.getRankService() != null, "Rank Service");
        check(sender, coreApi != null && coreApi.getVoucherPermissionService() != null, "Voucher Permission Service");
        check(sender, Bukkit.getServicesManager().load(IslandAccessService.class) != null, "Island Access API");
        check(sender, Bukkit.getServicesManager().load(PlotAccessService.class) != null, "Plot Access API");
        check(sender, CombatTagServiceImpl.liveInstance() != null, "CombatTag Live Service");
        check(sender, EventParticipationService.global() != null, "Event Participation Runtime");
        DiscordNotifier discord = Bukkit.getServicesManager().load(DiscordNotifier.class);
        check(sender, discord != null, "Discord Bridge");

        sender.sendMessage(ChatColor.AQUA + "Kritische Commands");
        String[] commands = {
                "is", "plot", "warp", "craterewards", "battlepass", "premiumpass", "quests", "kit", "prefix",
                "duel", "lms", "clanwar", "eventarena", "skymap", "casino", "jackpot", "playershop",
                "verlosen", "freitag", "casinonpc"
        };
        for (String commandName : commands) command(sender, commandName);

        sender.sendMessage(ChatColor.AQUA + "Offizielle Maps");
        check(sender, Bukkit.getWorld("SkyPvP") != null, "SkyPvP Produktionswelt");
        check(sender, Bukkit.getWorld("SkyIslands") != null, "SkyIslands Welt");
        check(sender, Bukkit.getWorld("SkyPlots") != null, "SkyPlots Welt");
        check(sender, Bukkit.getWorld("SkyCommunityEvent") != null, "SkyCommunityEvent Welt");

        sender.sendMessage(ChatColor.AQUA + "Persistenz & Recovery");
        eventReturnRecovery(sender);
        jackpotRecovery(sender);
        fileCheck(sender, "SkyKings-Core", "island-starter-claims.txt", "Island Starter-Claim Store", true);
        fileCheck(sender, "SkyKings-Crates", "issued-items.txt", "Issued Item Registry", true);
        fileCheck(sender, "SkyKings-Crates", "redeemed-vouchers.txt", "Voucher Redemption Store", true);

        EventParticipationService participation = EventParticipationService.global();
        int eventPlayers = participation == null ? 0 : participation.snapshot().size();
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
        sender.sendMessage(ChatColor.DARK_GRAY + "Runtime-Gate: danach /is delete+create, Crate, Voucher, Giveaway, Casino und Multiplayer-Flows manuell pruefen.");
        return true;
    }

    private void eventReturnRecovery(CommandSender sender) {
        boolean installed = EventReturnRecoveryService.isInstalled();
        check(sender, installed, "Event Return Recovery");
        if (!installed) return;

        int pending = EventReturnRecoveryService.pendingCount();
        if (pending > 0) {
            sender.sendMessage(ChatColor.YELLOW + "[RECOVERY]" + ChatColor.GRAY + " Event-Rueckkehrpositionen warten: " + ChatColor.WHITE + pending);
        } else {
            sender.sendMessage(ChatColor.GREEN + "[OK]" + ChatColor.GRAY + " Event Return Queue leer");
        }
    }

    private void jackpotRecovery(CommandSender sender) {
        Plugin core = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
        if (core == null) {
            check(sender, false, "Jackpot Recovery Status");
            return;
        }

        File file = new File(core.getDataFolder(), "jackpot.yml");
        if (!file.exists()) {
            check(sender, true, "Jackpot Recovery Status");
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String status = data.getString("recovery.status", "");
        if ("REVIEW_REQUIRED".equalsIgnoreCase(status)) {
            String winner = data.getString("recovery.winner-name", data.getString("recovery.winner", "unknown"));
            long payout = data.getLong("recovery.payout", 0L);
            sender.sendMessage(ChatColor.RED + "[REVIEW]" + ChatColor.GRAY + " Jackpot Recovery erforderlich"
                    + ChatColor.DARK_GRAY + " | Gewinner " + winner + " | Payout " + payout);
            return;
        }
        check(sender, true, "Jackpot Recovery Status");
    }

    private void fileCheck(CommandSender sender, String pluginName, String fileName, String label, boolean optionalBeforeFirstUse) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) { check(sender, false, label); return; }
        File file = new File(plugin.getDataFolder(), fileName);
        if (file.exists()) sender.sendMessage(ChatColor.GREEN + "[OK]" + ChatColor.GRAY + " " + label);
        else if (optionalBeforeFirstUse) sender.sendMessage(ChatColor.YELLOW + "[NEU]" + ChatColor.GRAY + " " + label + " wird beim ersten Einsatz angelegt");
        else check(sender, false, label);
    }

    private void plugin(CommandSender sender, String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        check(sender, plugin != null && plugin.isEnabled(), name);
    }

    private void command(CommandSender sender, String name) {
        PluginCommand command = Bukkit.getPluginCommand(name);
        check(sender, command != null && command.getPlugin().isEnabled(), "/" + name);
    }

    private void check(CommandSender sender, boolean ok, String name) { sender.sendMessage(status(ok) + " " + name); }
    private String status(boolean ok) { return ok ? ChatColor.GREEN + "[OK]" + ChatColor.GRAY : ChatColor.RED + "[FEHLT]" + ChatColor.GRAY; }
}
