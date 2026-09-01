package net.skykings.core.kit;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.model.Rank;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Kit-Arsenal statt klassischer Item-Wand: alle Progression-Stufen bleiben sichtbar,
 * READY/COOLDOWN/LOCKED sind sofort lesbar und jedes Kit hat eine echte Inhaltsvorschau.
 */
public final class KitGui {
    private static final int[] CARD_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34
    };
    private static final int PER_PAGE = CARD_SLOTS.length;

    private final GuiManager guiManager;
    private final KitGrantService kitGrantService;
    private final CooldownService cooldownService;

    public KitGui(GuiManager guiManager, KitGrantService kitGrantService, CooldownService cooldownService) {
        this.guiManager = guiManager;
        this.kitGrantService = kitGrantService;
        this.cooldownService = cooldownService;
    }

    public void open(Player player) {
        open(player, 0);
    }

    private void open(Player player, int requestedPage) {
        List<KitDefinition> accessible = sorted(kitGrantService.getAccessibleKits(player));
        List<KitDefinition> all = sorted(kitGrantService.getAllKits());
        if (all.isEmpty()) all = new ArrayList<KitDefinition>(accessible);

        final Set<String> accessibleIds = new HashSet<String>();
        for (KitDefinition kit : accessible) accessibleIds.add(normalize(kit.getId()));

        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / PER_PAGE;
        final int page = Math.max(0, Math.min(maxPage, requestedPage));
        GuiSession gui = GuiSession.create(player,
                UiTheme.title("Kit Arsenal" + (maxPage > 0 ? " " + (page + 1) + "/" + (maxPage + 1) : "")), 54);

        gui.setItem(4, UiItems.item(Material.CHEST, UiTheme.PRIMARY.toString() + ChatColor.BOLD + "DEIN KIT ARSENAL",
                UiTheme.MUTED + "Freigeschaltet " + UiTheme.TEXT + accessible.size() + "/" + all.size(),
                UiTheme.MUTED + "Links: abholen  " + UiTheme.DISABLED + "| " + UiTheme.MUTED + "Rechts: Vorschau",
                UiTheme.MUTED + "Hoehere Raenge schalten weitere Kits frei."));

        int start = page * PER_PAGE;
        for (int column = 0; column < PER_PAGE; column++) {
            int index = start + column;
            if (index >= all.size()) break;
            final KitDefinition kit = all.get(index);
            final boolean unlocked = accessibleIds.contains(normalize(kit.getId()));
            gui.setItem(CARD_SLOTS[column], buildIcon(player, kit, unlocked), (clicker,event,slot) -> {
                if (event.isRightClick()) {
                    openPreview(clicker, kit, unlocked, page);
                    return;
                }
                if (!unlocked) {
                    clicker.sendMessage(UiTheme.DANGER + "Kit gesperrt" + UiTheme.MUTED + " • benoetigt "
                            + colorFor(kit.getRequiredRank()) + prettyRank(kit.getRequiredRank()));
                    SoundFeedback.error(clicker);
                    return;
                }
                grantAndRefresh(clicker, kit, page);
            });
        }

        if (all.isEmpty()) gui.setItem(22, UiItems.empty("Keine Kits", "Aktuell sind keine Rank-Kits registriert."));

        gui.setItem(47, UiItems.item(Material.EMERALD, UiTheme.SUCCESS + "READY",
                UiTheme.MUTED + "Kann sofort abgeholt werden."));
        gui.setItem(49, UiItems.item(Material.WATCH, UiTheme.WARNING + "COOLDOWN",
                UiTheme.MUTED + "Bereits genutzt. Timer laeuft."));
        gui.setItem(51, UiItems.item(Material.BARRIER, UiTheme.DISABLED + "LOCKED",
                UiTheme.MUTED + "Hoeherer Gameplay-Rang erforderlich."));

        if (page > 0) {
            gui.setItem(46, UiItems.item(Material.ARROW, UiTheme.MUTED + "Vorherige Seite"),
                    (p,e,s) -> open(p, page - 1));
        }
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> {
            SoundFeedback.back(p);
            org.bukkit.Bukkit.dispatchCommand(p, "commands");
        });
        if (page < maxPage) {
            gui.setItem(UiTheme.NAV_NEXT, UiItems.next(), (p,e,s) -> open(p, page + 1));
        }
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void openPreview(Player player, KitDefinition kit, boolean unlocked, int returnPage) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Kit • " + ChatColor.stripColor(display(kit))), 54);
        long remaining = remaining(player, kit);

        gui.setItem(4, UiItems.item(materialFor(kit.getRequiredRank()), display(kit),
                UiTheme.MUTED + "Rang " + colorFor(kit.getRequiredRank()) + prettyRank(kit.getRequiredRank()),
                UiTheme.MUTED + "Cooldown " + UiTheme.TEXT + formatDuration(kit.getCooldownMillis()),
                unlocked ? (remaining <= 0L ? UiTheme.STATUS_READY : UiTheme.STATUS_COOLDOWN) : UiTheme.STATUS_LOCKED));

        int slot = 9;
        for (ItemStack original : kit.createItems()) {
            if (slot >= 45) break;
            if (original == null) continue;
            gui.setItem(slot++, original.clone());
        }
        for (PotionEffect effect : kit.getPotionEffects()) {
            if (slot >= 45) break;
            gui.setItem(slot++, UiItems.item(Material.POTION, UiTheme.MYTHIC + prettyEffect(effect),
                    UiTheme.MUTED + "Staerke " + UiTheme.TEXT + (effect.getAmplifier() + 1),
                    UiTheme.MUTED + "Dauer " + UiTheme.TEXT + (effect.getDuration() / 20) + "s"));
        }

        if (!unlocked) {
            gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.BARRIER,
                    UiTheme.DISABLED + "KIT LOCKED",
                    UiTheme.MUTED + "Benoetigt " + colorFor(kit.getRequiredRank()) + prettyRank(kit.getRequiredRank())));
        } else if (remaining > 0L) {
            gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.WATCH,
                    UiTheme.WARNING + "COOLDOWN",
                    UiTheme.MUTED + "Noch " + UiTheme.TEXT + formatDuration(remaining)));
        } else {
            gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.CHEST,
                    UiTheme.SUCCESS.toString() + ChatColor.BOLD + "KIT ABHOLEN",
                    UiTheme.MUTED + "Inhalt wird direkt deinem Inventar hinzugefuegt.",
                    UiItems.action("Klicken zum Abholen")),
                    (p,e,s) -> grantAndRefresh(p, kit, returnPage));
        }

        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p, returnPage));
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void grantAndRefresh(Player player, KitDefinition kit, int page) {
        KitGrantResult result = kitGrantService.grant(player, kit.getId());
        switch (result.getStatus()) {
            case SUCCESS:
                player.sendMessage(UiTheme.SUCCESS + "Kit " + ChatColor.stripColor(display(kit)) + " erhalten.");
                SoundFeedback.reward(player);
                open(player, page);
                break;
            case COOLDOWN:
                player.sendMessage(UiTheme.WARNING + "Noch " + formatDuration(result.getRemainingMillis()) + " Cooldown.");
                SoundFeedback.error(player);
                open(player, page);
                break;
            case INVENTORY_FULL:
                player.sendMessage(UiTheme.DANGER + "Du brauchst mehr freie Inventarplaetze.");
                SoundFeedback.error(player);
                break;
            case NO_PERMISSION:
                player.sendMessage(UiTheme.DANGER + "Dein Rang ist fuer dieses Kit nicht hoch genug.");
                SoundFeedback.error(player);
                open(player, page);
                break;
            default:
                player.sendMessage(UiTheme.DANGER + "Das Kit konnte nicht vergeben werden.");
                SoundFeedback.error(player);
                break;
        }
    }

    private ItemStack buildIcon(Player player, KitDefinition kit, boolean unlocked) {
        long remaining = remaining(player, kit);
        boolean ready = unlocked && remaining <= 0L;
        String nameColor = !unlocked ? UiTheme.DISABLED.toString() : ready ? UiTheme.SUCCESS.toString() : UiTheme.WARNING.toString();

        List<String> lore = new ArrayList<String>();
        lore.add(UiTheme.MUTED + "Rang " + colorFor(kit.getRequiredRank()) + prettyRank(kit.getRequiredRank()));
        lore.add(UiTheme.MUTED + "Cooldown " + UiTheme.TEXT + formatDuration(kit.getCooldownMillis()));
        lore.add("");
        if (!unlocked) {
            lore.add(UiTheme.STATUS_LOCKED);
            lore.add(UiTheme.MUTED + "Rechtsklick: Inhalt ansehen");
        } else if (ready) {
            lore.add(UiTheme.STATUS_READY);
            lore.add(UiItems.action("Linksklick: abholen"));
            lore.add(UiTheme.MUTED + "Rechtsklick: Vorschau");
        } else {
            lore.add(UiTheme.STATUS_COOLDOWN);
            lore.add(UiTheme.MUTED + "Noch " + formatDuration(remaining));
            lore.add(UiTheme.MUTED + "Rechtsklick: Vorschau");
        }
        return UiItems.item(materialFor(kit.getRequiredRank()), nameColor + ChatColor.stripColor(display(kit)),
                lore.toArray(new String[lore.size()]));
    }

    private long remaining(Player player, KitDefinition kit) {
        return cooldownService.getRemainingMillis(player.getUniqueId(), "kit:" + normalize(kit.getId()));
    }

    private List<KitDefinition> sorted(Collection<KitDefinition> source) {
        List<KitDefinition> kits = source == null
                ? new ArrayList<KitDefinition>() : new ArrayList<KitDefinition>(source);
        kits.sort(Comparator.comparingInt(kit -> kit.getRequiredRank().getTier()));
        return kits;
    }

    private String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private Material materialFor(Rank rank) {
        switch (rank) {
            case SPIELER: return Material.COAL;
            case IRON: return Material.IRON_INGOT;
            case GOLD: return Material.GOLD_INGOT;
            case EPIC: return Material.ENDER_PEARL;
            case DIAMOND: return Material.DIAMOND;
            case KNIGHT: return Material.IRON_SWORD;
            case PHOENIX: return Material.BLAZE_POWDER;
            case ETERNAL: return Material.NETHER_STAR;
            case EXILE: return Material.OBSIDIAN;
            case ENDLING: return Material.ENDER_PORTAL_FRAME;
            case KING: return Material.GOLDEN_APPLE;
            default: return Material.CHEST;
        }
    }

    private String colorFor(Rank rank) {
        switch (rank) {
            case SPIELER: return ChatColor.GRAY.toString();
            case IRON: return ChatColor.WHITE.toString();
            case GOLD: return ChatColor.GOLD.toString();
            case EPIC: return ChatColor.DARK_PURPLE.toString();
            case DIAMOND: return ChatColor.AQUA.toString();
            case KNIGHT: return ChatColor.BLUE.toString();
            case PHOENIX: return ChatColor.RED.toString();
            case ETERNAL: return ChatColor.LIGHT_PURPLE.toString();
            case EXILE: return ChatColor.DARK_AQUA.toString();
            case ENDLING: return ChatColor.DARK_GRAY.toString();
            case KING: return ChatColor.YELLOW.toString();
            default: return ChatColor.WHITE.toString();
        }
    }

    private String prettyRank(Rank rank) {
        String raw = rank.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private String prettyEffect(PotionEffect effect) {
        String raw = effect.getType().getName().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private String display(KitDefinition kit) {
        return ChatColor.translateAlternateColorCodes('&', kit.getDisplayName());
    }

    private String formatDuration(long millis) {
        if (millis <= 0L) return "bereit";
        long totalSeconds = Math.max(1L, (millis + 999L) / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
