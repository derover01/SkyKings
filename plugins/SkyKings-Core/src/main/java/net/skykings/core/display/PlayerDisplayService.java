package net.skykings.core.display;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Baut Chat-Prefixe und die bewusst reduzierte Tab-Anzeige. */
public final class PlayerDisplayService {

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

    public PlayerDisplayService(PlayerProfileService profileService, RankDisplayConfig displayConfig) {
        this.profileService = profileService;
        this.displayConfig = displayConfig;
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

    /** Tab bleibt absichtlich clean: nur Rang + Spielername, keine kosmetischen Prefixe. */
    public void refreshTab(Player player) {
        String listName = rankPrefixFor(player) + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + player.getName();
        if (listName.length() > 32) listName = listName.substring(0, 32);
        player.setPlayerListName(listName);
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
