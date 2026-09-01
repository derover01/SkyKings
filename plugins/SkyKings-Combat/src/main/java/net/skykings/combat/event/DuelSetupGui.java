package net.skykings.combat.event;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.kit.KitDefinition;
import net.skykings.core.kit.KitRegistry;
import net.skykings.core.model.Rank;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 54er Custom-Panel fuer faire Duel-Challenges mit identischem Kit fuer beide Spieler. */
public final class DuelSetupGui {
    private static final long[] WAGERS = {
            0L, 10_000L, 50_000L, 100_000L, 250_000L, 500_000L,
            1_000_000L, 5_000_000L, 10_000_000L, 25_000_000L
    };
    private static final int[] KIT_SLOTS = {19,20,21,22,23,24,25,28,29,30,31};

    private final DuelService duels;
    private final GuiManager guiManager;
    private final KitRegistry kits;

    public DuelSetupGui(DuelService duels, GuiManager guiManager, KitRegistry kits) {
        this.duels = duels;
        this.guiManager = guiManager;
        this.kits = kits;
    }

    public void open(Player challenger, Player target) {
        String defaultKit = kits != null && kits.contains("spieler") ? "spieler" : firstKitId();
        if (defaultKit == null) {
            challenger.sendMessage(UiTheme.DANGER + "Keine Duel-Kits verfuegbar.");
            SoundFeedback.error(challenger);
            return;
        }
        open(challenger, target, defaultKit, 0);
    }

    private void open(Player challenger, Player target, String selectedKit, int wagerIndex) {
        if (target == null || !target.isOnline()) {
            challenger.closeInventory();
            challenger.sendMessage(UiTheme.DANGER + "Der Spieler ist nicht mehr online.");
            SoundFeedback.error(challenger);
            return;
        }
        if (duels.isBusy(challenger.getUniqueId()) || duels.isBusy(target.getUniqueId())) {
            challenger.closeInventory();
            challenger.sendMessage(UiTheme.DANGER + "Einer von euch ist bereits in einem Event.");
            SoundFeedback.error(challenger);
            return;
        }

        final int safeWagerIndex = Math.max(0, Math.min(WAGERS.length - 1, wagerIndex));
        final long wager = WAGERS[safeWagerIndex];
        KitDefinition selected = kit(selectedKit);
        if (selected == null) {
            String fallback = firstKitId();
            if (fallback == null) return;
            selectedKit = fallback;
            selected = kit(fallback);
        }
        final String chosenKit = selectedKit;

        GuiSession gui = GuiSession.create(challenger, UiTheme.title("Duel Setup"), 54);
        gui.setItem(4, UiItems.item(Material.DIAMOND_SWORD,
                UiTheme.PRIMARY + "DUEL SETUP",
                UiTheme.MUTED + "Gegner " + UiTheme.TEXT + target.getName(),
                UiTheme.MUTED + "Beide spielen mit exakt demselben Kit."), null);

        gui.setItem(10, UiItems.head(target.getName(),
                UiTheme.TEXT + target.getName(),
                UiTheme.MUTED + "Dein Duel-Gegner",
                UiTheme.SUCCESS + "ONLINE"), null);
        gui.setItem(13, UiItems.item(icon(selected),
                UiTheme.PRIMARY + "Kit: " + selected.getDisplayName(),
                UiTheme.MUTED + "Rang-Kit dient nur als Loadout.",
                UiTheme.MUTED + "Besitz des Rangs ist im Duel egal."), null);
        gui.setItem(16, UiItems.item(Material.GOLD_INGOT,
                UiTheme.LEGENDARY + "Einsatz: " + (wager == 0L ? "Keiner" : UiFormat.coins(wager)),
                UiTheme.MUTED + "Beide zahlen denselben Betrag.",
                UiTheme.MUTED + "Gewinner erhaelt den kompletten Pot."), null);

        List<KitDefinition> ordered = orderedKits();
        for (int i = 0; i < ordered.size() && i < KIT_SLOTS.length; i++) {
            final KitDefinition definition = ordered.get(i);
            boolean active = definition.getId().equalsIgnoreCase(chosenKit);
            gui.setItem(KIT_SLOTS[i], UiItems.item(icon(definition),
                    (active ? UiTheme.PRIMARY : UiTheme.TEXT) + definition.getDisplayName(),
                    active ? UiTheme.SUCCESS + "SELECTED" : UiTheme.WARNING + "READY",
                    UiTheme.MUTED + "Fair: beide erhalten dieses Loadout.",
                    active ? UiTheme.SUCCESS + "Aktuell ausgewaehlt" : UiItems.action("Klicken zum Auswaehlen")),
                    (p,e,s) -> {
                        SoundFeedback.click(p);
                        open(p, target, definition.getId(), safeWagerIndex);
                    });
        }

        gui.setItem(37, UiItems.item(Material.REDSTONE,
                UiTheme.WARNING + "Einsatz senken",
                UiTheme.MUTED + "Aktuell: " + (wager == 0L ? "0 Coins" : UiFormat.coins(wager)),
                UiItems.action("Klicken")), (p,e,s) -> {
            SoundFeedback.click(p);
            open(p, target, chosenKit, Math.max(0, safeWagerIndex - 1));
        });

        gui.setItem(40, UiItems.item(Material.EMERALD_BLOCK,
                UiTheme.SUCCESS + "CHALLENGE SENDEN",
                UiTheme.MUTED + "Kit: " + UiTheme.TEXT + selected.getDisplayName(),
                UiTheme.MUTED + "Einsatz: " + UiTheme.TEXT + (wager == 0L ? "Keiner" : UiFormat.coins(wager)),
                UiItems.action("Klicken zum Herausfordern")), (p,e,s) -> {
            p.closeInventory();
            if (!duels.request(p, target, "duel", wager, chosenKit)) {
                p.sendMessage(UiTheme.DANGER + "Duel-Anfrage nicht moeglich.");
                p.sendMessage(UiTheme.MUTED + "Pruefe Coins, Combat-Status und Event-Status.");
                SoundFeedback.error(p);
            }
        });

        gui.setItem(43, UiItems.item(Material.GLOWSTONE_DUST,
                UiTheme.WARNING + "Einsatz erhoehen",
                UiTheme.MUTED + "Aktuell: " + (wager == 0L ? "0 Coins" : UiFormat.coins(wager)),
                UiItems.action("Klicken")), (p,e,s) -> {
            SoundFeedback.click(p);
            open(p, target, chosenKit, Math.min(WAGERS.length - 1, safeWagerIndex + 1));
        });

        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> {
            SoundFeedback.back(p);
            p.closeInventory();
        });
        gui.setItem(UiTheme.NAV_HOME, UiItems.home(), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "commands");
        });

        guiManager.open(gui);
        SoundFeedback.menuOpen(challenger);
    }

    private KitDefinition kit(String id) {
        if (kits == null || id == null || !kits.get(id).isPresent()) return null;
        return kits.get(id).get();
    }

    private List<KitDefinition> orderedKits() {
        if (kits == null) return Collections.emptyList();
        List<KitDefinition> out = new ArrayList<KitDefinition>(kits.getAll());
        Collections.sort(out, new Comparator<KitDefinition>() {
            @Override public int compare(KitDefinition a, KitDefinition b) {
                return Integer.compare(a.getRequiredRank().getTier(), b.getRequiredRank().getTier());
            }
        });
        return out;
    }

    private String firstKitId() {
        List<KitDefinition> ordered = orderedKits();
        return ordered.isEmpty() ? null : ordered.get(0).getId();
    }

    private Material icon(KitDefinition kit) {
        Rank rank = kit.getRequiredRank();
        switch (rank) {
            case SPIELER: return Material.LEATHER_CHESTPLATE;
            case IRON: return Material.IRON_CHESTPLATE;
            case GOLD: return Material.GOLD_CHESTPLATE;
            case EPIC: return Material.BLAZE_POWDER;
            case DIAMOND: return Material.DIAMOND_CHESTPLATE;
            case KNIGHT: return Material.IRON_SWORD;
            case PHOENIX: return Material.FIREBALL;
            case ETERNAL: return Material.EYE_OF_ENDER;
            case EXILE: return Material.OBSIDIAN;
            case ENDLING: return Material.ENDER_PEARL;
            case KING: return Material.GOLD_HELMET;
            default: return Material.CHEST;
        }
    }
}
