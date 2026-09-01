package net.skykings.core.retention;

import net.skykings.core.ui.UiTheme;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /dailyrewards sowie der separat geroutete /jackpot-Alias. */
public final class DailyRewardCommand implements CommandExecutor {
    private final DailyRewardService rewards;

    public DailyRewardCommand(DailyRewardService rewards) { this.rewards = rewards; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;

        if ("jackpot".equalsIgnoreCase(label)) {
            return handleJackpot(player, args);
        }

        if (rewards.claim(player)) return true;
        long seconds = rewards.secondsUntilNext(player.getUniqueId());
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        player.sendMessage(ChatColor.YELLOW + "Daily bereits abgeholt. " + ChatColor.GRAY + "Naechster Reward in "
                + ChatColor.WHITE + hours + "h " + minutes + "m" + ChatColor.GRAY + ". Streak: "
                + ChatColor.GOLD + rewards.getStreak(player.getUniqueId()) + "/7");
        return true;
    }

    private boolean handleJackpot(Player player, String[] args) {
        JackpotService jackpot = rewards.getJackpotService();
        if (args.length == 0) {
            jackpot.open(player);
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(UiTheme.WARNING + "Nutze: /jackpot [Coins]");
            return true;
        }
        try {
            long amount = Long.parseLong(args[0].replace(".", "").replace(",", ""));
            if (jackpot.enter(player, amount)) jackpot.open(player);
        } catch (NumberFormatException ex) {
            player.sendMessage(UiTheme.DANGER + "Bitte gib einen gueltigen Coin-Betrag an.");
        }
        return true;
    }
}
