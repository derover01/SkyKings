package net.skykings.core.perk;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.item.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Mehrseitige Unlimited-Baublöcke fuer Phoenix+; alle Items tragen eine No-Sell-Markierung. */
public final class BuildBlocksGui {

    public static final String NO_SELL_LORE = ChatColor.DARK_GRAY + "SkyKings Baublock • nicht verkäuflich";
    private static final int PAGE_SIZE = 45;

    private final GuiManager guiManager;
    private final List<BlockEntry> entries = new ArrayList<BlockEntry>();

    public BuildBlocksGui(GuiManager guiManager) {
        this.guiManager = guiManager;
        addDefaults();
    }

    public void open(Player player) { open(player, 0); }

    private void open(final Player player, final int page) {
        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(maxPage, page));
        GuiSession session = GuiSession.create(player,
                ChatColor.DARK_AQUA + "Baublöcke " + ChatColor.GRAY + (safePage + 1) + "/" + (maxPage + 1), 54);
        int from = safePage * PAGE_SIZE;
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        for (int i = from; i < to; i++) {
            final BlockEntry entry = entries.get(i);
            int slot = i - from;
            ItemStack icon = new ItemBuilder(entry.material)
                    .durability(entry.data)
                    .name(entry.name)
                    .lore("&7Klick: &f64 Blöcke erhalten", "&8Kostenlos • kein Economy-Wert")
                    .build();
            session.setItem(slot, icon, (p, e, s) -> give(p, entry));
        }
        if (safePage > 0) {
            session.setItem(45, new ItemBuilder(Material.ARROW).name("&e← Vorherige Seite").build(),
                    (p,e,s) -> open(p, safePage - 1));
        }
        session.setItem(49, new ItemBuilder(Material.BOOK).name("&bSkyKings Baublöcke")
                .lore("&7So viele sinnvolle Baublöcke wie möglich.", "&cBedrock, Beacon, Command Blocks usw. sind gesperrt.").build());
        if (safePage < maxPage) {
            session.setItem(53, new ItemBuilder(Material.ARROW).name("&eNächste Seite →").build(),
                    (p,e,s) -> open(p, safePage + 1));
        }
        guiManager.open(session);
    }

    private void give(Player player, BlockEntry entry) {
        ItemStack stack = new ItemBuilder(entry.material, 64)
                .durability(entry.data)
                .name(entry.name)
                .lore(NO_SELL_LORE)
                .build();
        if (!player.getInventory().addItem(stack).isEmpty()) {
            player.sendMessage(ChatColor.RED + "Du brauchst mindestens einen freien Inventarplatz.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "64 " + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', entry.name)) + " erhalten.");
    }

    public static boolean isNoSellBuildBlock(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasLore()
                && item.getItemMeta().getLore().contains(NO_SELL_LORE);
    }

    private void addDefaults() {
        add(Material.STONE, 0, "&7Stein"); add(Material.STONE, 1, "&7Granit"); add(Material.STONE, 3, "&7Diorit"); add(Material.STONE, 5, "&7Andesit");
        add(Material.COBBLESTONE, 0, "&7Bruchstein"); add(Material.MOSSY_COBBLESTONE, 0, "&2Moosiger Bruchstein");
        add(Material.DIRT, 0, "&6Erde"); add(Material.DIRT, 1, "&6Grobe Erde"); add(Material.GRASS, 0, "&aGrasblock");
        add(Material.SAND, 0, "&eSand"); add(Material.SAND, 1, "&cRoter Sand"); add(Material.GRAVEL, 0, "&7Kies");
        add(Material.SANDSTONE, 0, "&eSandstein"); add(Material.SANDSTONE, 1, "&eGemeißelter Sandstein"); add(Material.SANDSTONE, 2, "&eGlatter Sandstein");
        add(Material.RED_SANDSTONE, 0, "&cRoter Sandstein"); add(Material.RED_SANDSTONE, 1, "&cGemeißelter roter Sandstein"); add(Material.RED_SANDSTONE, 2, "&cGlatter roter Sandstein");
        add(Material.BRICK, 0, "&cZiegel"); add(Material.NETHER_BRICK, 0, "&4Netherziegel"); add(Material.QUARTZ_BLOCK, 0, "&fQuarzblock"); add(Material.QUARTZ_BLOCK, 1, "&fGemeißelter Quarz"); add(Material.QUARTZ_BLOCK, 2, "&fQuarzsäule");
        add(Material.WOOD, 0, "&6Eichenbretter"); add(Material.WOOD, 1, "&6Fichtenbretter"); add(Material.WOOD, 2, "&6Birkenbretter"); add(Material.WOOD, 3, "&6Dschungelbretter"); add(Material.WOOD, 4, "&6Akazienbretter"); add(Material.WOOD, 5, "&6Schwarzeichenbretter");
        add(Material.LOG, 0, "&6Eichenholz"); add(Material.LOG, 1, "&6Fichtenholz"); add(Material.LOG, 2, "&6Birkenholz"); add(Material.LOG, 3, "&6Dschungelholz"); add(Material.LOG_2, 0, "&6Akazienholz"); add(Material.LOG_2, 1, "&6Schwarzeichenholz");
        add(Material.GLASS, 0, "&fGlas");
        for (int i = 0; i < 16; i++) add(Material.STAINED_GLASS, i, "&bGefärbtes Glas " + i);
        for (int i = 0; i < 16; i++) add(Material.WOOL, i, "&fWolle " + i);
        for (int i = 0; i < 16; i++) add(Material.STAINED_CLAY, i, "&6Keramik " + i);
        add(Material.HARD_CLAY, 0, "&6Gebrannter Ton"); add(Material.BOOKSHELF, 0, "&6Bücherregal");
        add(Material.PRISMARINE, 0, "&3Prismarin"); add(Material.PRISMARINE, 1, "&3Prismarinziegel"); add(Material.PRISMARINE, 2, "&3Dunkles Prismarin"); add(Material.SEA_LANTERN, 0, "&bSeelaterne");
        add(Material.PACKED_ICE, 0, "&bPackeis"); add(Material.ICE, 0, "&bEis"); add(Material.SNOW_BLOCK, 0, "&fSchneeblock");
        add(Material.COAL_BLOCK, 0, "&8Kohleblock"); add(Material.IRON_BLOCK, 0, "&fEisenblock"); add(Material.GOLD_BLOCK, 0, "&6Goldblock"); add(Material.LAPIS_BLOCK, 0, "&9Lapislazuliblock"); add(Material.REDSTONE_BLOCK, 0, "&cRedstoneblock"); add(Material.EMERALD_BLOCK, 0, "&aSmaragdblock"); add(Material.DIAMOND_BLOCK, 0, "&bDiamantblock");
        add(Material.OBSIDIAN, 0, "&5Obsidian"); add(Material.GLOWSTONE, 0, "&eGlowstone"); add(Material.HAY_BLOCK, 0, "&eHeuballen"); add(Material.MELON_BLOCK, 0, "&aMelonenblock"); add(Material.PUMPKIN, 0, "&6Kürbis"); add(Material.JACK_O_LANTERN, 0, "&6Kürbislaterne");
        add(Material.SMOOTH_BRICK, 0, "&7Steinziegel"); add(Material.SMOOTH_BRICK, 1, "&2Moosige Steinziegel"); add(Material.SMOOTH_BRICK, 2, "&7Rissige Steinziegel"); add(Material.SMOOTH_BRICK, 3, "&7Gemeißelte Steinziegel");
        add(Material.FENCE, 0, "&6Eichenzaun"); add(Material.NETHER_FENCE, 0, "&4Netherzaun"); add(Material.COBBLE_WALL, 0, "&7Bruchsteinmauer"); add(Material.COBBLE_WALL, 1, "&2Moosige Mauer");
    }

    private void add(Material material, int data, String name) {
        entries.add(new BlockEntry(material, (short) data, name));
    }

    private static final class BlockEntry {
        final Material material;
        final short data;
        final String name;
        BlockEntry(Material material, short data, String name) { this.material = material; this.data = data; this.name = name; }
    }
}
