package net.skykings.core.display;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.clan.ClanService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Baut Chat-Prefixe, Clan-Tags und die bewusst reduzierte Tab-Anzeige. */
public final class PlayerDisplayService {

    static final int TAB_NAME_LIMIT = 32;

    private static final Prefix[] PREFIXES = new Prefix[] {
            new Prefix("kingkiller", ChatColor.DARK_RED + "KingKiller"),
            new Prefix("legend", ChatColor.GOLD + "Legend"),
            new Prefix("royal", ChatColor.LIGHT_PURPLE + "Royal"),
            new Prefix("lucky", ChatColor.GREEN + "Lucky"),
            new Prefix("hunter", ChatColor.DARK_AQUA + "Hunter"),
            new Prefix("fighter", ChatColor.RED + "Fighter")
    };

    private final PlayerProfileService profileService;
    private final RankDisplayConfig displayConfig;
    private final ChatDisplayPreferenceStore preferences;
    private volatile ClanService clanService;

    public PlayerDisplayService(PlayerProfileService profileService, RankDisplayConfig displayConfig) {
        this(profileService, displayConfig, null);
    }

    public PlayerDisplayService(PlayerProfileService profileService, RankDisplayConfig displayConfig, ClanService clanService) {
        this.profileService = profileService;
        this.displayConfig = displayConfig;
        this.clanService = clanService;
        this.preferences = new ChatDisplayPreferenceStore(JavaPlugin.getProvidingPlugin(PlayerDisplayService.class));
    }

    /** Chat-Identitaet: Cosmetic und Rang sind zwei unabhaengige Layer. */
    public String prefixFor(Player player) {
        StringBuilder out = new StringBuilder();
        String cosmetic = cosmeticPrefix(player);
        if (cosmetic != null && preferences.showCosmeticPrefix(player.getUniqueId())) {
            out.append(ChatColor.DARK_GRAY).append("[").append(cosmetic).append(ChatColor.DARK_GRAY).append("]");
        }
        if (preferences.showRank(player.getUniqueId())) {
            if (out.length() > 0) out.append(" ");
            out.append(rankPrefixFor(player));
        }
        return out.toString();
    }

    public String cosmeticPrefixFor(Player player) {
        String cosmetic = cosmeticPrefix(player);
        return cosmetic == null ? null : cosmetic;
    }

    /** Clan ist ein separater Identitaets-Layer und veraendert Rang/Prefix-Einstellungen nicht. */
    public String clanTagFor(Player player) {
        if (player == null) return "";
        ClanService service = resolveClanService();
        if (service == null) return "";
        ClanService.Clan clan = service.getClan(player.getUniqueId());
        if (clan == null || clan.getTag() == null || clan.getTag().isEmpty()) return "";
        return ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + clan.getTag() + ChatColor.DARK_GRAY + "]";
    }

    public boolean isCosmeticPrefixShown(Player player) {
        return preferences.showCosmeticPrefix(player.getUniqueId());
    }

    public void setCosmeticPrefixShown(Player player, boolean show) {
        preferences.setShowCosmeticPrefix(player.getUniqueId(), show);
    }

    public boolean isRankShown(Player player) {
        return preferences.showRank(player.getUniqueId());
    }

    public void setRankShown(Player player, boolean show) {
        preferences.setShowRank(player.getUniqueId(), show);
    }

    /** Backward-compatible Namen fuer bestehenden Code. */
    public boolean isRankShownWithCosmetic(Player player) {
        return isRankShown(player);
    }

    /** Backward-compatible Namen fuer bestehenden Code. */
    public void setRankShownWithCosmetic(Player player, boolean show) {
        setRankShown(player, show);
    }

    public boolean isClanTagShown(Player player) {
        return preferences.showClanTag(player.getUniqueId());
    }

    public void setClanTagShown(Player player, boolean show) {
        preferences.setShowClanTag(player.getUniqueId(), show);
    }

    /**
     * Tab bleibt bewusst kompakt: Rang + vollstaendiger Spielername.
     * Clan-Tags bleiben dort grundsaetzlich draussen, weil Rang + Clan + Name unter 1.8
     * den Spielernamen sonst abschneiden kann. Chat-Praeferenzen beeinflussen den Tab nicht.
     */
    public void refreshTab(Player player) {
        player.setPlayerListName(formatTabName(rankPrefixFor(player), player.getName()));
    }

    static String formatTabName(String rankPrefix, String playerName) {
        String safeName = playerName == null ? "" : playerName;
        String safeRank = rankPrefix == null ? "" : rankPrefix;
        String listName = safeRank + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + safeName;

        // Farben zaehlen fuer das Protokoll-Limit mit. Wenn Rang + Name zu lang sind,
        // faellt die Tab-Anzeige lieber auf den kompletten Spielernamen zurueck.
        if (listName.length() > TAB_NAME_LIMIT) {
            return ChatColor.WHITE + safeName;
        }
        return listName;
    }

    private ClanService resolveClanService() {
        ClanService current = clanService;
        if (current != null) return current;
        try {
            SkyKingsCoreAPI api = Bukkit.getServicesManager().load(SkyKingsCoreAPI.class);
            if (api != null) clanService = api.getClanService();
        } catch (Throwable ignored) {
            return null;
        }
        return clanService;
    }

    private String rankPrefixFor(Player player) {
        if (displayConfig.isConfiguredOwner(player.getName())) return displayConfig.getOwnerPrefix();
        PlayerProfile profile = profileService.getCached(player.getUniqueId());
        return profile == null ? ChatColor.GRAY + "Spieler" : displayConfig.getRankPrefix(profile.getRank());
    }

    private String cosmeticPrefix(Player player) {
        for (Prefix prefix : PREFIXES) {
            if (player.hasPermission("skykings.prefix." + prefix.id)) return prefix.display;
        }
        return null;
    }

    private static final class Prefix {
        final String id;
        final String display;
        Prefix(String id, String display) {
            this.id = id;
            this.display = display;
        }
    }
}
