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

    private static final int TAB_NAME_LIMIT = 32;

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

    /** Chat: Rang bleibt sichtbar, auch wenn der kosmetische Prefix bewusst ausgeblendet wurde. */
    public String prefixFor(Player player) {
        String rankPrefix = rankPrefixFor(player);
        String cosmetic = cosmeticPrefix(player);
        if (cosmetic == null || !preferences.showCosmeticPrefix(player.getUniqueId())) return rankPrefix;
        String cosmeticBlock = ChatColor.DARK_GRAY + "[" + cosmetic + ChatColor.DARK_GRAY + "]";
        return preferences.showRankWithCosmeticPrefix(player.getUniqueId())
                ? cosmeticBlock + " " + rankPrefix
                : cosmeticBlock;
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

    public boolean isRankShownWithCosmetic(Player player) {
        return preferences.showRankWithCosmeticPrefix(player.getUniqueId());
    }

    public void setRankShownWithCosmetic(Player player, boolean show) {
        preferences.setShowRankWithCosmeticPrefix(player.getUniqueId(), show);
    }

    /**
     * Tab bleibt bewusst kompakt: Rang + vollstaendiger Spielername.
     * Clan-Tags bleiben im Chat/Clan-System, weil Rang + Clan + Name unter 1.8
     * den Spielernamen sonst abschneiden kann. Der Name selbst wird niemals gekuerzt.
     */
    public void refreshTab(Player player) {
        String playerName = player.getName();
        String listName = rankPrefixFor(player) + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + playerName;

        // Farben zaehlen fuer das Protokoll-Limit mit. Wenn Rang + Name zu lang sind,
        // faellt die Tab-Anzeige lieber auf den kompletten Spielernamen zurueck.
        if (listName.length() > TAB_NAME_LIMIT) {
            listName = ChatColor.WHITE + playerName;
        }
        player.setPlayerListName(listName);
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
