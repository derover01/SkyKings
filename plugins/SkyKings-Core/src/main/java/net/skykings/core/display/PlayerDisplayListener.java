package net.skykings.core.display;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Locale;

public final class PlayerDisplayListener implements Listener {

    private final PlayerDisplayService displayService;

    public PlayerDisplayListener(PlayerDisplayService displayService) {
        this.displayService = displayService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        displayService.refreshTab(event.getPlayer());
    }

    /**
     * /prefix ist bewusst eine kleine Anzeige-Oberflaeche. Der kosmetische Prefix bleibt durch
     * seine Permission bestimmt; der Spieler entscheidet hier nur, ob der Rang daneben im Chat steht.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrefixCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2) return;
        String[] parts = raw.substring(1).trim().split("\\s+");
        if (parts.length == 0 || !parts[0].equalsIgnoreCase("prefix")) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (displayService.cosmeticPrefixFor(player) == null) {
            player.sendMessage(UiTheme.DANGER + "Du besitzt aktuell keinen kosmetischen Prefix.");
            SoundFeedback.error(player);
            return;
        }

        if (parts.length >= 2) {
            String option = parts[1].toLowerCase(Locale.ROOT);
            if ("an".equals(option) || "on".equals(option)) {
                displayService.setRankShownWithCosmetic(player, true);
                player.sendMessage(UiTheme.SUCCESS + "Rang im Chat-Prefix: AN");
                SoundFeedback.success(player);
                return;
            }
            if ("aus".equals(option) || "off".equals(option)) {
                displayService.setRankShownWithCosmetic(player, false);
                player.sendMessage(UiTheme.SUCCESS + "Rang im Chat-Prefix: AUS");
                SoundFeedback.success(player);
                return;
            }
            if ("toggle".equals(option)) {
                displayService.setRankShownWithCosmetic(player, !displayService.isRankShownWithCosmetic(player));
                openPrefixMenu(player);
                return;
            }
        }
        openPrefixMenu(player);
    }

    private void openPrefixMenu(Player player) {
        String cosmetic = displayService.cosmeticPrefixFor(player);
        boolean showRank = displayService.isRankShownWithCosmetic(player);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Chat Prefix"), 27);
        gui.setItem(11, UiItems.item(Material.NAME_TAG,
                UiTheme.PRIMARY + "Aktiver Prefix",
                UiTheme.TEXT + cosmetic,
                UiTheme.MUTED + "Kosmetisch und permanent."));
        gui.setItem(15, UiItems.item(showRank ? Material.EMERALD : Material.REDSTONE,
                showRank ? UiTheme.SUCCESS + "Rang anzeigen: AN" : UiTheme.DANGER + "Rang anzeigen: AUS",
                UiTheme.MUTED + "Nur fuer die Chat-Anzeige.",
                "",
                UiItems.action("Klicken zum Umschalten")), (p,e,s) -> {
            displayService.setRankShownWithCosmetic(p, !displayService.isRankShownWithCosmetic(p));
            SoundFeedback.success(p);
            openPrefixMenu(p);
        });
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "commands");
        });
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String prefix = displayService.prefixFor(player);
        event.setFormat(prefix + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "%1$s"
                + ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "%2$s");
    }
}
