package net.skykings.combat.cosmetic;

import net.skykings.combat.cosmetic.KillCosmeticService.KillEffect;
import net.skykings.combat.kill.KillMessageService;
import net.skykings.combat.kill.KillMessageService.MessageStyle;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Custom-Panel Cosmetics Center für Kill-Effects und Death-Messages. */
public final class KillEffectGui {

    private enum Tab { EFFECTS, MESSAGES }

    private final GuiManager guiManager;
    private final KillCosmeticService service;

    public KillEffectGui(GuiManager guiManager, KillCosmeticService service) {
        this.guiManager = guiManager;
        this.service = service;
    }

    public void open(Player player) {
        open(player, Tab.EFFECTS);
    }

    private void open(Player player, Tab tab) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Cosmetics Center"), 54);

        gui.setItem(4, UiItems.item(Material.NETHER_STAR,
                UiTheme.PRIMARY + "COSMETICS CENTER",
                UiTheme.MUTED + "Dein visueller PvP-Stil.",
                UiTheme.TEXT + "Keine Gameplay-Vorteile."), null);

        gui.setItem(11, UiItems.item(Material.BLAZE_POWDER,
                tab == Tab.EFFECTS ? UiTheme.PRIMARY + "Kill Effects • ACTIVE" : UiTheme.TEXT + "Kill Effects",
                UiTheme.MUTED + "Effekt direkt nach deinem Kill.",
                tab == Tab.EFFECTS ? UiTheme.SUCCESS + "Ausgewählter Bereich" : UiItems.action("Klicken zum Öffnen")),
                (p,e,s) -> { SoundFeedback.click(p); open(p, Tab.EFFECTS); });

        gui.setItem(15, UiItems.item(Material.PAPER,
                tab == Tab.MESSAGES ? UiTheme.PRIMARY + "Death Messages • ACTIVE" : UiTheme.TEXT + "Death Messages",
                UiTheme.MUTED + "Dein Stil in der Kill-Nachricht.",
                tab == Tab.MESSAGES ? UiTheme.SUCCESS + "Ausgewählter Bereich" : UiItems.action("Klicken zum Öffnen")),
                (p,e,s) -> { SoundFeedback.click(p); open(p, Tab.MESSAGES); });

        if (tab == Tab.EFFECTS) renderEffects(gui, player);
        else renderMessages(gui, player);

        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "commands");
        });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.COMPASS, UiTheme.PRIMARY + "Home",
                UiTheme.MUTED + "Zur SkyKings Übersicht.", UiItems.action("Klicken")), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "commands");
        });

        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void renderEffects(GuiSession gui, Player player) {
        addEffect(gui, player, 20, Material.BARRIER, KillEffect.NONE);
        addEffect(gui, player, 21, Material.BLAZE_POWDER, KillEffect.FLAME);
        addEffect(gui, player, 22, Material.RED_ROSE, KillEffect.HEART);
        addEffect(gui, player, 23, Material.ENDER_PEARL, KillEffect.ENDER);
        addEffect(gui, player, 24, Material.NETHER_STAR, KillEffect.LIGHTNING);

        KillEffect selected = service.getSelected(player.getUniqueId());
        gui.setItem(31, UiItems.item(Material.NAME_TAG,
                UiTheme.TEXT + "Aktiver Kill Effect",
                UiTheme.PRIMARY + selected.getDisplayName(),
                UiTheme.MUTED + "Wird direkt nach einem Kill abgespielt."), null);
    }

    private void addEffect(GuiSession gui, Player player, int slot, Material material, KillEffect effect) {
        boolean unlocked = service.canUse(player, effect);
        boolean selected = service.getSelected(player.getUniqueId()) == effect;
        String state = selected ? UiTheme.SUCCESS + "SELECTED"
                : unlocked ? UiTheme.WARNING + "READY" : UiTheme.MUTED + "LOCKED";
        gui.setItem(slot, UiItems.item(material,
                (selected ? UiTheme.PRIMARY : UiTheme.TEXT) + effect.getDisplayName(),
                state,
                effect == KillEffect.NONE ? UiTheme.MUTED + "Deaktiviert deinen Kill Effect."
                        : UiTheme.MUTED + "Rein kosmetischer PvP-Effekt.",
                selected ? UiTheme.SUCCESS + "Aktuell ausgewählt"
                        : unlocked ? UiItems.action("Klicken zum Auswählen") : UiTheme.DANGER + "Noch nicht freigeschaltet"),
                (p,e,s) -> {
                    if (!service.select(p, effect)) {
                        SoundFeedback.error(p);
                        p.sendMessage(UiTheme.DANGER + "Diesen Kill Effect hast du noch nicht freigeschaltet.");
                        return;
                    }
                    SoundFeedback.success(p);
                    open(p, Tab.EFFECTS);
                });
    }

    private void renderMessages(GuiSession gui, Player player) {
        KillMessageService messages = KillMessageService.liveInstance();
        if (messages == null) {
            gui.setItem(22, UiItems.item(Material.BARRIER, UiTheme.DANGER + "Death Messages nicht verfügbar",
                    UiTheme.MUTED + "Der Runtime-Service wurde nicht geladen."), null);
            return;
        }

        addMessage(gui, player, messages, 20, Material.PAPER, MessageStyle.CLASSIC,
                "Neutraler SkyKings PvP-Stil.");
        addMessage(gui, player, messages, 21, Material.GOLD_INGOT, MessageStyle.ROYAL,
                "Krone, Thron und Royal-Vibes.");
        addMessage(gui, player, messages, 22, Material.ENDER_PEARL, MessageStyle.VOID,
                "Dunkler Void-/Sky-Stil.");
        addMessage(gui, player, messages, 23, Material.BOW, MessageStyle.HUNTER,
                "Jagd- und Target-Vibes.");
        addMessage(gui, player, messages, 24, Material.NETHER_STAR, MessageStyle.LEGEND,
                "Seltene Legend-Nachrichten.");

        MessageStyle selected = messages.getSelected(player.getUniqueId());
        gui.setItem(31, UiItems.item(Material.BOOK,
                UiTheme.TEXT + "Aktive Death Message",
                selected.getColor() + selected.getDisplayName(),
                UiTheme.MUTED + "Wird bei deinen Open-World-PvP-Kills genutzt."), null);
    }

    private void addMessage(GuiSession gui, Player player, KillMessageService messages, int slot,
                            Material material, MessageStyle style, String description) {
        boolean unlocked = messages.canUse(player, style);
        boolean selected = messages.getSelected(player.getUniqueId()) == style;
        String state = selected ? UiTheme.SUCCESS + "SELECTED"
                : unlocked ? UiTheme.WARNING + "READY" : UiTheme.MUTED + "LOCKED";
        gui.setItem(slot, UiItems.item(material,
                (selected ? UiTheme.PRIMARY : style.getColor()) + style.getDisplayName(),
                state,
                UiTheme.MUTED + description,
                selected ? UiTheme.SUCCESS + "Aktuell ausgewählt"
                        : unlocked ? UiItems.action("Klicken zum Auswählen")
                        : UiTheme.DANGER + "Benötigt Freischaltung"),
                (p,e,s) -> {
                    if (!messages.select(p, style)) {
                        SoundFeedback.error(p);
                        p.sendMessage(UiTheme.DANGER + "Diese Death Message hast du noch nicht freigeschaltet.");
                        return;
                    }
                    SoundFeedback.success(p);
                    open(p, Tab.MESSAGES);
                });
    }
}
