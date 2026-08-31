package net.skykings.core.ui;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Einheitliches Confirmation-Pattern fuer riskante Aktionen. */
public final class ConfirmationMenu {
    private ConfirmationMenu() {}

    public static void open(Player player, ItemStack subject, String title, String description,
                            Runnable confirm, Runnable cancel) {
        GuiSession gui = GuiSession.create(player, UiTheme.title(title), 27);
        gui.setItem(13, subject == null
                ? UiItems.item(Material.PAPER, UiTheme.TEXT + title, UiTheme.MUTED + description)
                : subject);
        gui.setItem(11, UiItems.item(Material.WOOL, (short) 5,
                UiTheme.SUCCESS + "Bestaetigen",
                UiTheme.MUTED + description,
                "",
                UiItems.action("Klicken zum Bestaetigen")), (p,e,s) -> {
            p.closeInventory();
            SoundFeedback.confirm(p);
            if (confirm != null) confirm.run();
        });
        gui.setItem(15, UiItems.item(Material.WOOL, (short) 14,
                UiTheme.DANGER + "Abbrechen",
                UiTheme.MUTED + "Keine Aenderung wird vorgenommen."), (p,e,s) -> {
            p.closeInventory();
            SoundFeedback.back(p);
            if (cancel != null) cancel.run();
        });
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }
}
