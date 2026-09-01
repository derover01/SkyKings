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

    /** /prefix verwaltet nur die Anzeige; Besitz und Permissions des Prefixes bleiben unangetastet. */
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
                displayService.setCosmeticPrefixShown(player, true);
                player.sendMessage(UiTheme.SUCCESS + "Kosmetischer Prefix: AN");
                SoundFeedback.success(player);
                return;
            }
            if ("aus".equals(option) || "off".equals(option)) {
                displayService.setCosmeticPrefixShown(player, false);
                player.sendMessage(UiTheme.SUCCESS + "Kosmetischer Prefix: AUS");
                SoundFeedback.success(player);
                return;
            }
            if ("rang".equals(option) && parts.length >= 3) {
                String state = parts[2].toLowerCase(Locale.ROOT);
                if ("an".equals(state) || "on".equals(state)) displayService.setRankShownWithCosmetic(player, true);
                else if ("aus".equals(state) || "off".equals(state)) displayService.setRankShownWithCosmetic(player, false);
                else {
                    player.sendMessage(UiTheme.WARNING + "Nutze: /prefix rang <an|aus>");
                    return;
                }
                player.sendMessage(UiTheme.SUCCESS + "Rang neben Prefix: " + (displayService.isRankShownWithCosmetic(player) ? "AN" : "AUS"));
                SoundFeedback.success(player);
                return;
            }
        }
        openPrefixMenu(player);
    }

    private void openPrefixMenu(Player player) {
        String cosmetic = displayService.cosmeticPrefixFor(player);
        boolean showPrefix = displayService.isCosmeticPrefixShown(player);
        boolean showRank = displayService.isRankShownWithCosmetic(player);
        GuiSession gui = GuiSession.create(player, UiTheme.title("Chat Prefix"), 27);
        gui.setItem(11, UiItems.item(showPrefix ? Material.NAME_TAG : Material.INK_SACK,
                showPrefix ? UiTheme.SUCCESS + "Prefix: AN" : UiTheme.DANGER + "Prefix: AUS",
                UiTheme.TEXT + cosmetic,
                UiItems.action("Klicken zum Umschalten")), (p,e,s) -> {
            displayService.setCosmeticPrefixShown(p, !displayService.isCosmeticPrefixShown(p));
            SoundFeedback.success(p);
            openPrefixMenu(p);
        });
        gui.setItem(15, UiItems.item(showRank ? Material.EMERALD : Material.REDSTONE,
                showRank ? UiTheme.SUCCESS + "Rang daneben: AN" : UiTheme.DANGER + "Rang daneben: AUS",
                UiTheme.MUTED + "Wirkt nur bei sichtbarem Prefix.",
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
        String clan = displayService.clanTagFor(player);
        event.setFormat(prefix + ChatColor.DARK_GRAY + " | "
                + (clan.isEmpty() ? "" : clan + " ")
                + ChatColor.WHITE + "%1$s"
                + ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "%2$s");
    }
}
