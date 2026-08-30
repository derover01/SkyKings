package net.skykings.core.kit;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.item.ItemBuilder;
import net.skykings.core.model.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** GUI fuer alle Kits, die der aktuelle Rang beanspruchen darf. */
public final class KitGui {

    private final GuiManager guiManager;
    private final KitGrantService kitGrantService;
    private final CooldownService cooldownService;

    public KitGui(GuiManager guiManager, KitGrantService kitGrantService, CooldownService cooldownService) {
        this.guiManager = guiManager;
        this.kitGrantService = kitGrantService;
        this.cooldownService = cooldownService;
    }

    public void open(Player player) {
        Collection<KitDefinition> accessible = kitGrantService.getAccessibleKits(player);
        List<KitDefinition> kits = new ArrayList<>(accessible);
        kits.sort(Comparator.comparingInt(kit -> kit.getRequiredRank().ordinal()));

        int size = kits.size() <= 9 ? 18 : 27;
        GuiSession session = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Kits", size);
        for (int slot = 0; slot < size; slot++) {
            session.setItem(slot, new ItemBuilder(Material.STAINED_GLASS_PANE)
                    .durability((short) 15)
                    .name(" ")
                    .build());
        }

        int start = kits.size() <= 9 ? 4 - (kits.size() - 1) / 2 : 9;
        for (int i = 0; i < kits.size(); i++) {
            final KitDefinition kit = kits.get(i);
            int slot = kits.size() <= 9 ? start + i : 9 + i;
            session.setItem(slot, buildIcon(player, kit),
                    (clicker, event, clickedSlot) -> grantAndRefresh(clicker, kit));
        }

        if (kits.isEmpty()) {
            session.setItem(size / 2, new ItemBuilder(Material.BARRIER)
                    .name("&cKeine Kits verfuegbar")
                    .build());
        }

        guiManager.open(session);
    }

    private void grantAndRefresh(Player player, KitDefinition kit) {
        KitGrantResult result = kitGrantService.grant(player, kit.getId());
        switch (result.getStatus()) {
            case SUCCESS:
                player.sendMessage(ChatColor.GREEN + "Kit " + display(kit) + ChatColor.GREEN + " erhalten.");
                open(player);
                break;
            case COOLDOWN:
                player.sendMessage(ChatColor.RED + "Dieses Kit ist noch "
                        + formatDuration(result.getRemainingMillis()) + " im Cooldown.");
                open(player);
                break;
            case INVENTORY_FULL:
                player.sendMessage(ChatColor.RED + "Du brauchst mehr freie Inventarplaetze fuer dieses Kit.");
                break;
            case NO_PERMISSION:
                player.sendMessage(ChatColor.RED + "Dein Rang ist fuer dieses Kit nicht hoch genug.");
                break;
            case PROFILE_NOT_LOADED:
                player.sendMessage(ChatColor.RED + "Dein Spielerprofil ist noch nicht geladen.");
                break;
            case NOT_FOUND:
            default:
                player.sendMessage(ChatColor.RED + "Das Kit konnte nicht vergeben werden.");
                break;
        }
    }

    private ItemStack buildIcon(Player player, KitDefinition kit) {
        long remaining = cooldownService.getRemainingMillis(player.getUniqueId(),
                "kit:" + kit.getId().toLowerCase(Locale.ROOT));
        List<String> lore = new ArrayList<>();
        lore.add("&7Benötigter Rang: " + colorFor(kit.getRequiredRank()) + kit.getRequiredRank().name());
        lore.add("&7Cooldown: &f" + formatDuration(kit.getCooldownMillis()));
        lore.add("");
        if (remaining > 0L) {
            lore.add("&cNoch " + formatDuration(remaining));
        } else {
            lore.add("&aBereit - Klicke zum Abholen");
        }

        return new ItemBuilder(materialFor(kit.getRequiredRank()))
                .name(kit.getDisplayName())
                .lore(lore)
                .build();
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
            case SPIELER: return "&7";
            case IRON: return "&f";
            case GOLD: return "&6";
            case EPIC: return "&5";
            case DIAMOND: return "&b";
            case KNIGHT: return "&9";
            case PHOENIX: return "&c";
            case ETERNAL: return "&d";
            case EXILE: return "&3";
            case ENDLING: return "&8";
            case KING: return "&e";
            default: return "&f";
        }
    }

    private String display(KitDefinition kit) {
        return ChatColor.translateAlternateColorCodes('&', kit.getDisplayName());
    }

    private String formatDuration(long millis) {
        if (millis <= 0L) {
            return "bereit";
        }
        long totalSeconds = Math.max(1L, (millis + 999L) / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
