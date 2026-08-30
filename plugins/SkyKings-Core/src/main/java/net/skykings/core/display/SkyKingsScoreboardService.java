package net.skykings.core.display;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.text.NumberFormat;
import java.util.Locale;

/** Ästhetisches 1.8.8-PvP-Sidebar-Scoreboard mit persistenten Spielerstatistiken. */
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
        objective.setDisplayName(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKY" + ChatColor.YELLOW + ChatColor.BOLD + "KINGS");

        String rank = displayConfig.getRankPrefix(profile.getRank());
        if (displayConfig.isConfiguredOwner(player.getName())) rank = displayConfig.getOwnerPrefix();

        int kills = safeStatistic(player, Statistic.PLAYER_KILLS);
        int deaths = safeStatistic(player, Statistic.DEATHS);
        String kd = deaths <= 0 ? String.format(Locale.US, "%.2f", (double) kills)
                : String.format(Locale.US, "%.2f", (double) kills / (double) deaths);

        line(objective, ChatColor.DARK_GRAY + "──────────────", 12);
        line(objective, ChatColor.GRAY + "Rang  " + rank, 11);
        line(objective, ChatColor.GRAY + "Coins  " + ChatColor.GOLD + numbers.format(profile.getCoins()), 10);
        line(objective, ChatColor.BLACK.toString(), 9);
        line(objective, ChatColor.RED + "⚔ " + ChatColor.GRAY + "Kills  " + ChatColor.WHITE + kills, 8);
        line(objective, ChatColor.DARK_RED + "☠ " + ChatColor.GRAY + "Tode   " + ChatColor.WHITE + deaths, 7);
        line(objective, ChatColor.YELLOW + "K/D   " + ChatColor.WHITE + kd, 6);
        line(objective, ChatColor.DARK_GRAY.toString(), 5);
        line(objective, ChatColor.GREEN + "● " + ChatColor.GRAY + "Online " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size(), 4);
        line(objective, ChatColor.GRAY + "Du bist " + ChatColor.WHITE + shorten(player.getName(), 16), 3);
        line(objective, ChatColor.GRAY.toString(), 2);
        line(objective, ChatColor.GOLD + "OP SkyPvP", 1);

        player.setScoreboard(board);
    }

    private int safeStatistic(Player player, Statistic statistic) {
        try { return Math.max(0, player.getStatistic(statistic)); }
        catch (RuntimeException ignored) { return 0; }
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
