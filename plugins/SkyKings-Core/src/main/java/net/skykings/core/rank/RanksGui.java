package net.skykings.core.rank;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.item.ItemBuilder;
import net.skykings.core.model.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Interaktive Uebersicht aller Free- und Paid-Raenge. */
public final class RanksGui {

    private static final int[] RANK_SLOTS = {1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15};

    private final GuiManager guiManager;
    private final RankService rankService;
    private final RankProgressionService progressionService;
    private final RankProgressionConfig progressionConfig;
    private final EconomyService economyService;

    public RanksGui(GuiManager guiManager, RankService rankService,
                    RankProgressionService progressionService,
                    RankProgressionConfig progressionConfig,
                    EconomyService economyService) {
        this.guiManager = guiManager;
        this.rankService = rankService;
        this.progressionService = progressionService;
        this.progressionConfig = progressionConfig;
        this.economyService = economyService;
    }

    public void open(Player player) {
        Rank current = rankService.getRank(player.getUniqueId());
        RankProgressionResult preview = progressionService.preview(player.getUniqueId());
        GuiSession session = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Raenge", 27);

        for (int slot = 0; slot < 27; slot++) {
            session.setItem(slot, new ItemBuilder(Material.STAINED_GLASS_PANE)
                    .durability((short) 15)
                    .name(" ")
                    .build());
        }

        Rank[] ranks = Rank.values();
        for (int i = 0; i < ranks.length; i++) {
            final Rank rank = ranks[i];
            int slot = RANK_SLOTS[i];
            ItemStack icon = buildRankIcon(rank, current, preview);
            if (preview.getTargetRank() == rank && preview.getStatus() != RankProgressionResult.Status.PAID_RANK) {
                session.setItem(slot, icon, (clicker, event, clickedSlot) -> purchaseAndRefresh(clicker));
            } else {
                session.setItem(slot, icon);
            }
        }

        session.setItem(22, new ItemBuilder(Material.EMERALD)
                .name("&aDein Kontostand")
                .lore("&7Coins: &e" + format(economyService.getBalance(player.getUniqueId())))
                .build());

        guiManager.open(session);
    }

    private void purchaseAndRefresh(Player player) {
        RankProgressionResult result;
        try {
            result = progressionService.purchaseNext(player.getUniqueId());
        } catch (RuntimeException ex) {
            player.sendMessage(ChatColor.RED + "Der Rang konnte gerade nicht gekauft werden. Deine Coins wurden, wenn noetig, zurueckerstattet.");
            return;
        }

        switch (result.getStatus()) {
            case SUCCESS:
                player.sendMessage(ChatColor.GREEN + "Rang gekauft: " + ChatColor.YELLOW + result.getTargetRank().name()
                        + ChatColor.GRAY + " fuer " + ChatColor.GOLD + format(result.getCost()) + " Coins");
                open(player);
                break;
            case INSUFFICIENT_COINS:
                player.sendMessage(ChatColor.RED + "Dir fehlen Coins fuer " + result.getTargetRank().name() + ". Preis: "
                        + format(result.getCost()));
                open(player);
                break;
            case MAX_FREE_RANK:
                player.sendMessage(ChatColor.GREEN + "Du hast bereits den hoechsten Free-Rang erreicht.");
                break;
            case PAID_RANK:
                player.sendMessage(ChatColor.RED + "Paid-Raenge koennen nicht mit Coins gekauft werden.");
                break;
            default:
                break;
        }
    }

    private ItemStack buildRankIcon(Rank rank, Rank current, RankProgressionResult preview) {
        List<String> lore = new ArrayList<>();
        boolean reached = current.isAtLeast(rank);

        if (rank.isFree()) {
            if (reached) {
                lore.add("&aErreicht");
            } else {
                long cost = progressionConfig.getCost(rank);
                lore.add("&7Preis: &e" + format(cost) + " Coins");
                if (preview.getTargetRank() == rank) {
                    if (preview.getStatus() == RankProgressionResult.Status.SUCCESS) {
                        lore.add("&aKlicke zum Kaufen");
                    } else {
                        lore.add("&cNicht genug Coins");
                        lore.add("&7Klicke fuer Info");
                    }
                } else {
                    lore.add("&8Kaufe zuerst den vorherigen Rang");
                }
            }
        } else {
            lore.add(reached ? "&aDein Rang schaltet diesen Rang ein" : "&dPremium-Rang");
            lore.add("&7Nicht mit Coins kaufbar");
        }

        if (rank == current) {
            lore.add(0, "&e&lDEIN AKTUELLER RANG");
        }

        return new ItemBuilder(materialFor(rank))
                .name(colorFor(rank) + "&l" + rank.name())
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
            default: return Material.PAPER;
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

    private String format(long amount) {
        return NumberFormat.getIntegerInstance(Locale.GERMANY).format(amount);
    }
}
