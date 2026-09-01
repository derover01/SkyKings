package net.skykings.core.plot;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Kauf- und Auswahlmenue fuer dauerhafte Plot-Rand-Cosmetics. */
public final class PlotBorderMenu {
    private final PlotBorderService borders;
    private final GuiManager guiManager;

    public PlotBorderMenu(PlotBorderService borders) {
        this.borders = borders;
        this.guiManager = GuiManager.active();
    }

    public void open(Player player) {
        if (!borders.hasPlot(player.getUniqueId())) {
            player.sendMessage(UiTheme.DANGER + "Du besitzt noch keinen Plot.");
            return;
        }

        GuiSession gui = GuiSession.create(player, UiTheme.title("Plot Rand"), 54);
        PlotBorderTheme selected = borders.selected(player.getUniqueId());
        PlotBorderTheme[] themes = PlotBorderTheme.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 24};

        for (int i = 0; i < themes.length; i++) {
            final PlotBorderTheme theme = themes[i];
            boolean owned = borders.owns(player.getUniqueId(), theme);
            boolean active = selected == theme;
            String state = active ? UiTheme.SUCCESS + "ACTIVE"
                    : owned ? UiTheme.PRIMARY + "FREIGESCHALTET"
                    : UiTheme.WARNING + format(theme.getPrice()) + " Coins";
            String action = active ? UiTheme.DISABLED + "Bereits ausgewählt"
                    : owned ? UiItems.action("Klicken zum Auswählen")
                    : UiItems.action("Klicken zum Kaufen");

            gui.setItem(slots[i], UiItems.item(theme.getMaterial(),
                    (active ? UiTheme.SUCCESS : UiTheme.TEXT) + theme.getDisplayName(),
                    UiTheme.MUTED + "Rand innerhalb deines Plots.",
                    state,
                    "",
                    action), (p, e, s) -> {
                if (borders.selected(p.getUniqueId()) == theme) return;
                if (borders.owns(p.getUniqueId(), theme)) {
                    borders.selectOwned(p, theme);
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.7F, 1.35F);
                    open(p);
                    return;
                }
                openConfirm(p, theme);
            });
        }

        gui.setItem(40, UiItems.item(Material.GOLD_NUGGET, UiTheme.LEGENDARY + "Deine Coins",
                UiTheme.TEXT + format(borders.balance(player.getUniqueId())) + " Coins",
                UiTheme.MUTED + "Gekaufte Ränder bleiben dauerhaft."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> p.performCommand("plot"));
        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.25F);
    }

    private void openConfirm(Player player, final PlotBorderTheme theme) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Rand kaufen"), 27);
        gui.setItem(13, UiItems.item(theme.getMaterial(), UiTheme.TEXT + theme.getDisplayName(),
                UiTheme.MUTED + "Preis: " + UiTheme.WARNING + format(theme.getPrice()) + " Coins",
                UiTheme.MUTED + "Kontostand: " + UiTheme.TEXT + format(borders.balance(player.getUniqueId()))));
        gui.setItem(11, UiItems.item(Material.WOOL, UiTheme.SUCCESS + "KAUFEN",
                UiTheme.TEXT + format(theme.getPrice()) + " Coins",
                "", UiItems.action("Kauf bestätigen")), (p,e,s) -> {
            if (borders.purchaseAndSelect(p, theme)) {
                p.sendMessage(UiTheme.SUCCESS + theme.getDisplayName() + ChatColor.GRAY + " wurde dauerhaft freigeschaltet.");
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.8F, 1.3F);
            } else {
                p.sendMessage(UiTheme.DANGER + "Nicht genug Coins oder Plot nicht verfügbar.");
                p.playSound(p.getLocation(), Sound.VILLAGER_NO, 0.7F, 1.0F);
            }
            open(p);
        });
        gui.setItem(15, UiItems.item(Material.BARRIER, UiTheme.DANGER + "ABBRECHEN",
                UiTheme.MUTED + "Es werden keine Coins abgezogen."), (p,e,s) -> open(p));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
    }

    private String format(long value) {
        return String.format(java.util.Locale.GERMANY, "%,d", value);
    }
}
