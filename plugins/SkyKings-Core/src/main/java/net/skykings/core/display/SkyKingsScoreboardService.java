package net.skykings.core.display;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import net.skykings.core.pvp.PvpStatsProvider;
import net.skykings.core.pvp.PvpStatsSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.text.NumberFormat;
import java.util.Locale;

/** Ästhetisches 1.8.8-PvP-Sidebar-Scoreboard mit persistenten SkyKings-Stats und Nametags. */
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

        PvpStatsSnapshot stats = stats(player);
        String kd = String.format(Locale.US, "%.2f", stats.getKd());

        line(objective, ChatColor.DARK_GRAY + "──────────────", 14);
        line(objective, ChatColor.GRAY + "Rang  " + rank, 13);
        line(objective, ChatColor.GRAY + "Coins  " + ChatColor.GOLD + numbers.format(profile.getCoins()), 12);
        line(objective, ChatColor.BLACK.toString(), 11);
        line(objective, ChatColor.RED + "⚔ " + ChatColor.GRAY + "Kills  " + ChatColor.WHITE + stats.getKills(), 10);
        line(objective, ChatColor.DARK_RED + "☠ " + ChatColor.GRAY + "Tode   " + ChatColor.WHITE + stats.getDeaths(), 9);
        line(objective, ChatColor.YELLOW + "K/D   " + ChatColor.WHITE + kd, 8);
        line(objective, ChatColor.GOLD + "🔥 " + ChatColor.GRAY + "Streak " + ChatColor.WHITE + stats.getCurrentStreak(), 7);
        line(objective, ChatColor.YELLOW + "★ " + ChatColor.GRAY + "Best   " + ChatColor.WHITE + stats.getBestStreak(), 6);
        line(objective, ChatColor.DARK_GRAY.toString(), 5);
        line(objective, ChatColor.GREEN + "● " + ChatColor.GRAY + "Online " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size(), 4);
        line(objective, ChatColor.GRAY + "Du bist " + ChatColor.WHITE + shorten(player.getName(), 16), 3);
        line(objective, ChatColor.GRAY.toString(), 2);
        line(objective, ChatColor.GOLD + "OP SkyPvP", 1);

        applyNameTags(board);
        player.setScoreboard(board);
    }

    /**
     * In 1.8 werden Nametag-Prefixe ueber Scoreboard-Teams gerendert. Da SkyKings pro Viewer
     * ein eigenes Sidebar-Board verwendet, muessen die Teams auf genau diesem Board existieren.
     */
    private void applyNameTags(Scoreboard board) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            String rawPrefix = displayPrefix(target);
            String teamName = "nt" + target.getUniqueId().toString().replace("-", "").substring(0, 14);
            Team team = board.registerNewTeam(teamName);
            team.setPrefix(nametagPrefix(rawPrefix));
            team.addPlayer(target);
        }
    }

    private String displayPrefix(Player target) {
        if (displayConfig.isConfiguredOwner(target.getName())) return displayConfig.getOwnerPrefix();

        String primaryGroup = resolvePrimaryGroup(target);
        if (displayConfig.isTeamGroup(primaryGroup)) return displayConfig.getTeamPrefix(primaryGroup);

        PlayerProfile targetProfile = profileService.getCached(target.getUniqueId());
        return targetProfile == null ? ChatColor.GRAY + "SPIELER" : displayConfig.getRankPrefix(targetProfile.getRank());
    }

    private String resolvePrimaryGroup(Player player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user == null ? null : user.getPrimaryGroup();
        } catch (IllegalStateException unavailable) {
            return null;
        }
    }

    /** Team-Prefixe sind in Minecraft 1.8 hart auf 16 Zeichen begrenzt. */
    private String nametagPrefix(String raw) {
        String plain = ChatColor.stripColor(raw == null ? "" : raw);
        if ("Test-Supporter".equalsIgnoreCase(plain)) plain = "Test-Sup";

        ChatColor color = firstColor(raw);
        if (color == null) color = ChatColor.GRAY;

        // Immer Platz zwischen Rang und Name reservieren: 2 Farbcode + max. 13 Text + 1 Leerzeichen.
        if (plain.length() > 13) plain = plain.substring(0, 13);
        return color.toString() + plain + " ";
    }

    private ChatColor firstColor(String raw) {
        if (raw == null) return null;
        for (int i = 0; i + 1 < raw.length(); i++) {
            if (raw.charAt(i) != ChatColor.COLOR_CHAR) continue;
            char code = Character.toLowerCase(raw.charAt(i + 1));
            if ("0123456789abcdef".indexOf(code) < 0) continue;
            return ChatColor.getByChar(code);
        }
        return null;
    }

    private PvpStatsSnapshot stats(Player player) {
        RegisteredServiceProvider<PvpStatsProvider> registration =
                Bukkit.getServicesManager().getRegistration(PvpStatsProvider.class);
        if (registration == null || registration.getProvider() == null) {
            return new PvpStatsSnapshot(0L, 0L, 0, 0);
        }
        return registration.getProvider().getStats(player.getUniqueId());
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
