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
import java.util.List;
import java.util.Locale;

/** Moderner Kit-Hub mit READY/COOLDOWN-Zustaenden und eigener Kit-Vorschau. */
public final class KitGui {
    private final GuiManager guiManager;
    private final KitGrantService kitGrantService;
    private final CooldownService cooldownService;

    public KitGui(GuiManager guiManager, KitGrantService kitGrantService, CooldownService cooldownService) {
        this.guiManager = guiManager; this.kitGrantService = kitGrantService; this.cooldownService = cooldownService;
    }

    public void open(Player player) {
        Collection<KitDefinition> accessible = kitGrantService.getAccessibleKits(player);
        List<KitDefinition> kits = new ArrayList<KitDefinition>(accessible);
        kits.sort(Comparator.comparingInt(kit -> kit.getRequiredRank().ordinal()));

        GuiSession gui = GuiSession.create(player, UiTheme.title("Kits"), 54);
        gui.setItem(4, UiItems.item(Material.CHEST, UiTheme.PRIMARY + "Deine Kits",
                UiTheme.MUTED + "Linksklick: Kit abholen",
                UiTheme.MUTED + "Rechtsklick: Inhalt ansehen",
                "", UiTheme.TEXT.toString() + kits.size() + UiTheme.MUTED + " Kits freigeschaltet"));

        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        for (int i = 0; i < kits.size() && i < slots.length; i++) {
            final KitDefinition kit = kits.get(i);
            gui.setItem(slots[i], buildIcon(player, kit), (clicker,event,slot) -> {
                if (event.isRightClick()) openPreview(clicker, kit);
                else grantAndRefresh(clicker, kit);
            });
        }
        if (kits.isEmpty()) gui.setItem(22, UiItems.empty("Keine Kits", "Dein Rang hat aktuell keine Kits."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); org.bukkit.Bukkit.dispatchCommand(p, "commands"); });
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void openPreview(Player player, KitDefinition kit) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Kit Preview"), 54);
        gui.setItem(4, UiItems.item(materialFor(kit.getRequiredRank()), display(kit),
                UiTheme.MUTED + "Rang: " + colorFor(kit.getRequiredRank()) + prettyRank(kit.getRequiredRank()),
                UiTheme.MUTED + "Cooldown: " + UiTheme.TEXT + formatDuration(kit.getCooldownMillis()),
                "", UiItems.action("Linksklick im Kit-Menue zum Abholen")));

        int slot = 9;
        for (ItemStack original : kit.createItems()) {
            if (slot >= 45) break;
            if (original == null) continue;
            gui.setItem(slot++, original.clone());
        }
        for (PotionEffect effect : kit.getPotionEffects()) {
            if (slot >= 45) break;
            gui.setItem(slot++, UiItems.item(Material.POTION, UiTheme.MYTHIC + prettyEffect(effect),
                    UiTheme.MUTED + "Staerke: " + UiTheme.TEXT + (effect.getAmplifier() + 1),
                    UiTheme.MUTED + "Dauer: " + UiTheme.TEXT + (effect.getDuration() / 20) + "s"));
        }
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void grantAndRefresh(Player player, KitDefinition kit) {
        KitGrantResult result = kitGrantService.grant(player, kit.getId());
        switch (result.getStatus()) {
            case SUCCESS:
                player.sendMessage(UiTheme.SUCCESS + "Kit " + ChatColor.stripColor(display(kit)) + " erhalten.");
                SoundFeedback.reward(player); open(player); break;
            case COOLDOWN:
                player.sendMessage(UiTheme.DANGER + "Noch " + formatDuration(result.getRemainingMillis()) + " Cooldown.");
                SoundFeedback.error(player); open(player); break;
            case INVENTORY_FULL:
                player.sendMessage(UiTheme.DANGER + "Du brauchst mehr freie Inventarplaetze."); SoundFeedback.error(player); break;
            case NO_PERMISSION:
                player.sendMessage(UiTheme.DANGER + "Dein Rang ist fuer dieses Kit nicht hoch genug."); break;
            default:
                player.sendMessage(UiTheme.DANGER + "Das Kit konnte nicht vergeben werden."); break;
        }
    }

    private ItemStack buildIcon(Player player, KitDefinition kit) {
        long remaining = cooldownService.getRemainingMillis(player.getUniqueId(), "kit:" + kit.getId().toLowerCase(Locale.ROOT));
        boolean ready = remaining <= 0L;
        return UiItems.item(materialFor(kit.getRequiredRank()),
                (ready ? UiTheme.SUCCESS : UiTheme.WARNING) + ChatColor.stripColor(display(kit)),
                UiTheme.MUTED + "Rang: " + colorFor(kit.getRequiredRank()) + prettyRank(kit.getRequiredRank()),
                UiTheme.MUTED + "Cooldown: " + UiTheme.TEXT + formatDuration(kit.getCooldownMillis()),
                "",
                ready ? UiTheme.STATUS_READY : UiTheme.STATUS_COOLDOWN,
                ready ? UiItems.action("Linksklick: abholen") : UiTheme.MUTED + "Noch " + formatDuration(remaining),
                UiTheme.MUTED + "Rechtsklick: Vorschau");
    }

    private Material materialFor(Rank rank) {
        switch (rank) {
            case SPIELER: return Material.COAL; case IRON: return Material.IRON_INGOT; case GOLD: return Material.GOLD_INGOT;
            case EPIC: return Material.ENDER_PEARL; case DIAMOND: return Material.DIAMOND; case KNIGHT: return Material.IRON_SWORD;
            case PHOENIX: return Material.BLAZE_POWDER; case ETERNAL: return Material.NETHER_STAR; case EXILE: return Material.OBSIDIAN;
            case ENDLING: return Material.ENDER_PORTAL_FRAME; case KING: return Material.GOLDEN_APPLE; default: return Material.CHEST;
        }
    }

    private String colorFor(Rank rank) {
        switch (rank) {
            case SPIELER: return ChatColor.GRAY.toString(); case IRON: return ChatColor.WHITE.toString(); case GOLD: return ChatColor.GOLD.toString();
            case EPIC: return ChatColor.DARK_PURPLE.toString(); case DIAMOND: return ChatColor.AQUA.toString(); case KNIGHT: return ChatColor.BLUE.toString();
            case PHOENIX: return ChatColor.RED.toString(); case ETERNAL: return ChatColor.LIGHT_PURPLE.toString(); case EXILE: return ChatColor.DARK_AQUA.toString();
            case ENDLING: return ChatColor.DARK_GRAY.toString(); case KING: return ChatColor.YELLOW.toString(); default: return ChatColor.WHITE.toString();
        }
    }

    private String prettyRank(Rank rank) {
        String raw = rank.name().toLowerCase(Locale.ROOT); return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
    private String prettyEffect(PotionEffect effect) {
        String raw = effect.getType().getName().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
    private String display(KitDefinition kit) { return ChatColor.translateAlternateColorCodes('&', kit.getDisplayName()); }
    private String formatDuration(long millis) {
        if (millis <= 0L) return "bereit";
        long totalSeconds = Math.max(1L, (millis + 999L) / 1000L); long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L; long seconds = totalSeconds % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
