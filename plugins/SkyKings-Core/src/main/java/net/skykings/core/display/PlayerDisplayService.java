package net.skykings.core.display;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/** Baut die sichtbare Kombination aus kosmetischem Prefix und Gameplay-/Team-Rang. */
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

    public PlayerDisplayService(PlayerProfileService profileService, RankDisplayConfig displayConfig) {
        this.profileService = profileService;
        this.displayConfig = displayConfig;
    }

    public String prefixFor(Player player) {
        String rankPrefix;
        if (displayConfig.isConfiguredOwner(player.getName())) {
            rankPrefix = displayConfig.getOwnerPrefix();
        } else {
            PlayerProfile profile = profileService.getCached(player.getUniqueId());
            rankPrefix = profile == null ? ChatColor.GRAY + "Spieler" : displayConfig.getRankPrefix(profile.getRank());
        }
        String cosmetic = cosmeticPrefix(player);
        return cosmetic == null ? rankPrefix : ChatColor.DARK_GRAY + "[" + cosmetic + ChatColor.DARK_GRAY + "] " + rankPrefix;
    }

    public void refreshTab(Player player) {
        String listName = prefixFor(player) + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + player.getName();
        if (listName.length() > 32) listName = listName.substring(0, 32);
        player.setPlayerListName(listName);
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
