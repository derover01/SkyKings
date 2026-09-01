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

    /** /prefix verwaltet ausschliesslich sichtbare Chat-Layer; Besitz/Clan/Rang bleiben unangetastet. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrefixCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2) return;
        String[] parts = raw.substring(1).trim().split("\\s+");
        if (parts.length == 0 || !parts[0].equalsIgnoreCase("prefix")) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (parts.length >= 2) {
            String option = parts[1].toLowerCase(Locale.ROOT);

            // Legacy-Kurzform: /prefix an|aus schaltet weiterhin nur den Cosmetic-Prefix.
            if ("an".equals(option) || "on".equals(option) || "aus".equals(option) || "off".equals(option)) {
                if (displayService.cosmeticPrefixFor(player) == null) {
                    player.sendMessage(UiTheme.DANGER + "Du besitzt aktuell keinen kosmetischen Prefix.");
                    SoundFeedback.error(player);
                    return;
                }
                boolean show = "an".equals(option) || "on".equals(option);
                displayService.setCosmeticPrefixShown(player, show);
                player.sendMessage(UiTheme.SUCCESS + "Kosmetischer Prefix: " + (show ? "AN" : "AUS"));
                SoundFeedback.success(player);
                return;
            }

            if (("prefix".equals(option) || "cosmetic".equals(option)) && parts.length >= 3) {
                if (displayService.cosmeticPrefixFor(player) == null) {
                    player.sendMessage(UiTheme.DANGER + "Du besitzt aktuell keinen kosmetischen Prefix.");
                    SoundFeedback.error(player);
                    return;
                }
                Boolean show = parseState(parts[2]);
                if (show == null) {
                    player.sendMessage(UiTheme.WARNING + "Nutze: /prefix prefix <an|aus>");
                    return;
                }
                displayService.setCosmeticPrefixShown(player, show.booleanValue());
                player.sendMessage(UiTheme.SUCCESS + "Kosmetischer Prefix: " + (show.booleanValue() ? "AN" : "AUS"));
                SoundFeedback.success(player);
                return;
            }

            if ("rang".equals(option) && parts.length >= 3) {
                Boolean show = parseState(parts[2]);
                if (show == null) {
                    player.sendMessage(UiTheme.WARNING + "Nutze: /prefix rang <an|aus>");
                    return;
                }
                displayService.setRankShown(player, show.booleanValue());
                player.sendMessage(UiTheme.SUCCESS + "Rang im Chat: " + (show.booleanValue() ? "AN" : "AUS"));
                SoundFeedback.success(player);
                return;
            }

            if ("clan".equals(option) && parts.length >= 3) {
                Boolean show = parseState(parts[2]);
                if (show == null) {
                    player.sendMessage(UiTheme.WARNING + "Nutze: /prefix clan <an|aus>");
                    return;
                }
                displayService.setClanTagShown(player, show.booleanValue());
                player.sendMessage(UiTheme.SUCCESS + "Clan-Tag im Chat: " + (show.booleanValue() ? "AN" : "AUS"));
                SoundFeedback.success(player);
                return;
            }
        }
        openPrefixMenu(player);
    }

    private Boolean parseState(String raw) {
        String state = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        if ("an".equals(state) || "on".equals(state)) return Boolean.TRUE;
        if ("aus".equals(state) || "off".equals(state)) return Boolean.FALSE;
        return null;
    }

    private void openPrefixMenu(Player player) {
        String cosmetic = displayService.cosmeticPrefixFor(player);
        boolean hasCosmetic = cosmetic != null;
        boolean showPrefix = hasCosmetic && displayService.isCosmeticPrefixShown(player);
        boolean showRank = displayService.isRankShown(player);
        boolean showClan = displayService.isClanTagShown(player);
        String clan = displayService.clanTagFor(player);

        GuiSession gui = GuiSession.create(player, UiTheme.title("Chat Identitaet"), 27);

        if (hasCosmetic) {
            gui.setItem(10, UiItems.item(showPrefix ? Material.NAME_TAG : Material.INK_SACK,
                    showPrefix ? UiTheme.SUCCESS + "Prefix: AN" : UiTheme.DANGER + "Prefix: AUS",
                    UiTheme.TEXT + cosmetic,
                    UiTheme.MUTED + "Nur Anzeige im Chat.",
                    UiItems.action("Klicken zum Umschalten")), (p,e,s) -> {
                displayService.setCosmeticPrefixShown(p, !displayService.isCosmeticPrefixShown(p));
                SoundFeedback.success(p);
                openPrefixMenu(p);
            });
        } else {
            gui.setItem(10, UiItems.item(Material.BARRIER,
                    UiTheme.MUTED + "Kein Cosmetic-Prefix",
                    UiTheme.MUTED + "Du besitzt aktuell keinen Prefix."));
        }

        gui.setItem(13, UiItems.item(showRank ? Material.EMERALD : Material.REDSTONE,
                showRank ? UiTheme.SUCCESS + "Rang im Chat: AN" : UiTheme.DANGER + "Rang im Chat: AUS",
                UiTheme.MUTED + "Unabhaengig vom Cosmetic-Prefix.",
                UiItems.action("Klicken zum Umschalten")), (p,e,s) -> {
            displayService.setRankShown(p, !displayService.isRankShown(p));
            SoundFeedback.success(p);
            openPrefixMenu(p);
        });

        gui.setItem(16, UiItems.item(showClan ? Material.BANNER : Material.REDSTONE,
                showClan ? UiTheme.SUCCESS + "Clan-Tag: AN" : UiTheme.DANGER + "Clan-Tag: AUS",
                clan.isEmpty() ? UiTheme.MUTED + "Aktuell keinem Clan zugeordnet." : UiTheme.TEXT + clan,
                UiTheme.MUTED + "Gilt nur fuer den Chat, nicht fuer den Tab.",
                UiItems.action("Klicken zum Umschalten")), (p,e,s) -> {
            displayService.setClanTagShown(p, !displayService.isClanTagShown(p));
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
        String clan = displayService.isClanTagShown(player) ? displayService.clanTagFor(player) : "";

        StringBuilder identity = new StringBuilder();
        if (!prefix.isEmpty()) identity.append(prefix);
        if (!clan.isEmpty()) {
            if (identity.length() > 0) identity.append(" ");
            identity.append(clan);
        }

        String leading = identity.length() == 0
                ? ""
                : identity.toString() + ChatColor.DARK_GRAY + " | ";
        event.setFormat(leading
                + ChatColor.WHITE + "%1$s"
                + ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "%2$s");
    }
}
