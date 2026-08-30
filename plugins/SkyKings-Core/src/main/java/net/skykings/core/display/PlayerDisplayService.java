package net.skykings.core.display;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class PlayerDisplayService {

    private final PlayerProfileService profileService;
    private final RankDisplayConfig displayConfig;

    public PlayerDisplayService(PlayerProfileService profileService, RankDisplayConfig displayConfig) {
        this.profileService = profileService;
        this.displayConfig = displayConfig;
    }

    public String prefixFor(Player player) {
        if (displayConfig.isConfiguredOwner(player.getName())) {
            return displayConfig.getOwnerPrefix();
        }
        PlayerProfile profile = profileService.getCached(player.getUniqueId());
        if (profile == null) {
            return ChatColor.GRAY + "SPIELER";
        }
        return displayConfig.getRankPrefix(profile.getRank());
    }

    public void refreshTab(Player player) {
        String listName = prefixFor(player) + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + player.getName();
        // 1.8-Clients haben fuer den PlayerListName eine harte Laengenbegrenzung.
        if (listName.length() > 32) {
            listName = listName.substring(0, 32);
        }
        player.setPlayerListName(listName);
    }
}
