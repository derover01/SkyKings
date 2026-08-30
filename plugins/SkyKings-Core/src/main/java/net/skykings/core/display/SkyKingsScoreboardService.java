package net.skykings.core.display;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.text.NumberFormat;
import java.util.Locale;

/** Kompaktes 1.8.8-Sidebar-Scoreboard mit Live-Spielerdaten. */
public final class SkyKingsScoreboardService {

    private final PlayerProfileService profileService;
    private final RankDisplayConfig displayConfig;
    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.GERMANY);

    public SkyKingsScoreboardService(PlayerProfileService profileService, RankDisplayConfig displayConfig) {
        this.profileService = profileService;
        this.displayConfig = displayConfig;
    }

    public void refresh(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerProfile profile = profileService.getCached(player.getUniqueId());
        if (profile == null) return;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("skykings", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS");

        String rank = displayConfig.getRankPrefix(profile.getRank());
        if (displayConfig.isConfiguredOwner(player.getName())) rank = displayConfig.getOwnerPrefix();

        line(objective, ChatColor.DARK_GRAY + "──────────────", 10);
        line(objective, ChatColor.GRAY + "Rang: " + rank, 9);
        line(objective, ChatColor.GRAY + "Coins: " + ChatColor.GOLD + numbers.format(profile.getCoins()), 8);
        line(objective, ChatColor.GRAY + "Nethersterne: " + ChatColor.AQUA + numbers.format(profile.getNetherstars()), 7);
        line(objective, ChatColor.BLACK.toString(), 6);
        line(objective, ChatColor.GRAY + "Online: " + ChatColor.GREEN + Bukkit.getOnlinePlayers().size(), 5);
        line(objective, ChatColor.GRAY + "Spieler: " + ChatColor.WHITE + shorten(player.getName(), 15), 4);
        line(objective, ChatColor.DARK_GRAY + " ", 3);
        line(objective, ChatColor.YELLOW + "play.skykings.de", 2);
        line(objective, ChatColor.DARK_GRAY + "─────────────", 1);

        player.setScoreboard(board);
    }

    private void line(Objective objective, String text, int score) {
        String line = text;
        if (line.length() > 40) line = line.substring(0, 40);
        objective.getScore(line).setScore(score);
    }

    private String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
