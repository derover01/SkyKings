package net.skykings.combat.community;

import net.skykings.core.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Einfache serverweite Verlosung mit Coins. */
public final class GiveawayCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final Set<UUID> joined = new HashSet<UUID>();
    private final Random random = new Random();
    private boolean active;
    private long reward;
    private int taskId = -1;

    public GiveawayCommand(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/verlosung join" + ChatColor.GRAY + " oder Admin: /verlosung start <Sekunden> <Coins>");
            return true;
        }
        if ("join".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (!active) { player.sendMessage(ChatColor.RED + "Aktuell laeuft keine Verlosung."); return true; }
            if (!joined.add(player.getUniqueId())) { player.sendMessage(ChatColor.YELLOW + "Du nimmst bereits teil."); return true; }
            player.sendMessage(ChatColor.GREEN + "Du nimmst an der Verlosung teil.");
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.5F, 1.3F);
            return true;
        }
        if ("start".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("skykings.admin.event")) { sender.sendMessage(ChatColor.RED + "Keine Berechtigung."); return true; }
            if (active) { sender.sendMessage(ChatColor.RED + "Es laeuft bereits eine Verlosung."); return true; }
            if (args.length < 3) { sender.sendMessage(ChatColor.RED + "/verlosung start <Sekunden> <Coins>"); return true; }
            try {
                int seconds = Math.max(10, Math.min(3600, Integer.parseInt(args[1])));
                reward = Math.max(1L, Long.parseLong(args[2]));
                active = true;
                joined.clear();
                Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "VERLOSUNG " + ChatColor.YELLOW
                        + reward + " Coins" + ChatColor.GRAY + " - /verlosung join - Ziehung in " + seconds + "s");
                taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::finish, seconds * 20L);
            } catch (NumberFormatException ex) { sender.sendMessage(ChatColor.RED + "Ungueltige Zahl."); }
            return true;
        }
        if ("stop".equalsIgnoreCase(args[0]) && sender.hasPermission("skykings.admin.event")) {
            if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
            active = false; joined.clear(); taskId = -1;
            Bukkit.broadcastMessage(ChatColor.RED + "Die Verlosung wurde abgebrochen.");
            return true;
        }
        return true;
    }

    private void finish() {
        taskId = -1;
        if (!active) return;
        active = false;
        List<UUID> candidates = new ArrayList<UUID>();
        for (UUID uuid : joined) if (Bukkit.getPlayer(uuid) != null) candidates.add(uuid);
        joined.clear();
        if (candidates.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Verlosung beendet - keine Teilnehmer online.");
            return;
        }
        UUID winnerId = candidates.get(random.nextInt(candidates.size()));
        Player winner = Bukkit.getPlayer(winnerId);
        economy.deposit(winnerId, reward, "GIVEAWAY", "Server-Verlosung");
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "VERLOSUNG: " + ChatColor.YELLOW
                + (winner == null ? winnerId.toString().substring(0, 8) : winner.getName()) + ChatColor.GRAY + " gewinnt " + reward + " Coins!");
        if (winner != null) winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1F, 1.2F);
    }
}
