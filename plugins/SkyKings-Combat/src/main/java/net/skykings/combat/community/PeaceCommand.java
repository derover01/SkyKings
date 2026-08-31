package net.skykings.combat.community;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** /peace <Spieler>|accept|deny|remove|list mit gegenseitiger Zustimmung. */
public final class PeaceCommand implements CommandExecutor {
    private final PeaceService peace;
    private final Map<UUID, UUID> requests = new HashMap<UUID, UUID>();

    public PeaceCommand(PeaceService peace) { this.peace = peace; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player player = (Player) sender;
        if (args.length == 0) { usage(player); return true; }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("list".equals(sub)) {
            player.sendMessage(ChatColor.GOLD + "Aktive Peace-Verbindungen: " + ChatColor.WHITE + peace.countFor(player.getUniqueId()));
            return true;
        }
        if ("accept".equals(sub)) {
            UUID from = requests.remove(player.getUniqueId());
            if (from == null) { player.sendMessage(ChatColor.RED + "Keine offene Peace-Anfrage."); return true; }
            peace.add(player.getUniqueId(), from);
            Player other = Bukkit.getPlayer(from);
            player.sendMessage(ChatColor.GREEN + "Frieden geschlossen" + (other == null ? "." : " mit " + other.getName() + "."));
            if (other != null) other.sendMessage(ChatColor.GREEN + player.getName() + " hat deine Peace-Anfrage angenommen.");
            return true;
        }
        if ("deny".equals(sub)) {
            requests.remove(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Peace-Anfrage abgelehnt.");
            return true;
        }
        if ("remove".equals(sub) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { player.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            if (peace.remove(player.getUniqueId(), target.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "Frieden mit " + target.getName() + " beendet.");
                target.sendMessage(ChatColor.YELLOW + player.getName() + " hat euren Frieden beendet.");
            } else player.sendMessage(ChatColor.RED + "Ihr habt keinen aktiven Frieden.");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || target.equals(player)) { player.sendMessage(ChatColor.RED + "Spieler nicht gefunden."); return true; }
        if (peace.isPeace(player.getUniqueId(), target.getUniqueId())) { player.sendMessage(ChatColor.YELLOW + "Ihr habt bereits Frieden."); return true; }
        requests.put(target.getUniqueId(), player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Peace-Anfrage an " + target.getName() + " gesendet.");
        target.sendMessage(ChatColor.AQUA + player.getName() + " moechte Frieden. " + ChatColor.GREEN + "/peace accept " + ChatColor.GRAY + "oder " + ChatColor.RED + "/peace deny");
        return true;
    }

    private void usage(Player p) {
        p.sendMessage(ChatColor.GOLD + "Peace-System");
        p.sendMessage(ChatColor.YELLOW + "/peace <Spieler>");
        p.sendMessage(ChatColor.YELLOW + "/peace accept | deny");
        p.sendMessage(ChatColor.YELLOW + "/peace remove <Spieler>");
        p.sendMessage(ChatColor.YELLOW + "/peace list");
    }
}
