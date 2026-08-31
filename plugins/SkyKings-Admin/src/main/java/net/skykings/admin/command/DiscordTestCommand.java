package net.skykings.admin.command;

import net.skykings.admin.discord.DiscordBridge;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** Testet die ausgehende Discord-Bot-Verbindung pro konfiguriertem Channel. */
public final class DiscordTestCommand implements CommandExecutor {
    private final DiscordBridge bridge;

    public DiscordTestCommand(DiscordBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skykings.admin.discord")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        String channel = args.length > 0 ? args[0].toLowerCase(java.util.Locale.ROOT) : "staff";
        if (!bridge.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Discord ist nicht aktiv. Setze enabled: true und die Umgebungsvariable SKYKINGS_DISCORD_BOT_TOKEN.");
            return true;
        }
        if (!bridge.isConfigured(channel)) {
            sender.sendMessage(ChatColor.RED + "Channel '" + channel + "' ist in discord.yml nicht korrekt konfiguriert.");
            return true;
        }
        bridge.send(channel, "✅ SkyKings Discord-Test erfolgreich. Quelle: /discordtest " + channel);
        sender.sendMessage(ChatColor.GREEN + "Discord-Test an Channel '" + channel + "' gesendet.");
        return true;
    }
}
