package net.skykings.combat.retention;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /quests zeigt Daily-/Weekly-Fortschritt. */
public final class QuestCommand implements CommandExecutor {
    private final QuestService quests;

    public QuestCommand(QuestService quests) { this.quests = quests; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player p = (Player) sender;
        java.util.UUID uuid = p.getUniqueId();
        p.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "QUESTS");
        line(p, "Daily PvP", quests.get(uuid, "daily.kills"), 5, quests.claimed(uuid, "daily.claimed-kills"));
        line(p, "Daily Pearls", quests.get(uuid, "daily.pearls"), 20, quests.claimed(uuid, "daily.claimed-pearls"));
        line(p, "Weekly PvP", quests.get(uuid, "weekly.kills"), 30, quests.claimed(uuid, "weekly.claimed-kills"));
        p.sendMessage(ChatColor.DARK_GRAY + "Rewards werden automatisch beim Abschluss ausgezahlt.");
        return true;
    }

    private void line(Player p, String name, int value, int target, boolean claimed) {
        int shown = Math.min(value, target);
        p.sendMessage((claimed ? ChatColor.GREEN : ChatColor.YELLOW) + name + ChatColor.GRAY + ": "
                + ChatColor.WHITE + shown + "/" + target + (claimed ? ChatColor.GREEN + " ✔" : ""));
    }
}
