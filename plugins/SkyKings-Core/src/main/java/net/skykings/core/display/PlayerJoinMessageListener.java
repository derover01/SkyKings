package net.skykings.core.display;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Stilvolle Join-Nachrichten fuer normale Spieler, Paid-Raenge, Team und Owner. */
public final class PlayerJoinMessageListener implements Listener {

    private final RankDisplayConfig displayConfig;
    private final RankService rankService;

    public PlayerJoinMessageListener(RankDisplayConfig displayConfig, RankService rankService) {
        this.displayConfig = displayConfig;
        this.rankService = rankService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();

        if (displayConfig.isConfiguredOwner(name)) {
            event.setJoinMessage(displayConfig.formatJoinMessage("owner", name,
                    displayConfig.getOwnerPrefix(), displayConfig.getOwnerPrefix()));
            return;
        }

        String primaryGroup = resolvePrimaryGroup(event);
        if (displayConfig.isTeamGroup(primaryGroup)) {
            event.setJoinMessage(displayConfig.formatJoinMessage("team", name, "",
                    displayConfig.getTeamPrefix(primaryGroup)));
            return;
        }

        try {
            Rank rank = rankService.getRank(event.getPlayer().getUniqueId());
            if (rank.isPaid()) {
                event.setJoinMessage(displayConfig.formatJoinMessage("paid", name,
                        displayConfig.getRankPrefix(rank), ""));
                return;
            }
        } catch (IllegalStateException ignored) {
            // Falls das Profil beim Join wider Erwarten noch nicht im Cache liegt, faellt die Anzeige sicher zurueck.
        }

        event.setJoinMessage(displayConfig.formatJoinMessage("normal", name, "", ""));
    }

    private String resolvePrimaryGroup(PlayerJoinEvent event) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(event.getPlayer().getUniqueId());
            return user == null ? null : user.getPrimaryGroup();
        } catch (IllegalStateException unavailable) {
            return null;
        }
    }
}
