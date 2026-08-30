package net.skykings.core.perk;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.item.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Unlimited Build-Block-Auswahl fuer Phoenix+; Items tragen eine No-Sell-Markierung in der Lore. */
public final class BuildBlocksGui {

    public static final String NO_SELL_LORE = ChatColor.DARK_GRAY + "SkyKings Buildblock - nicht verkaeuflich";

    private final GuiManager guiManager;

    public BuildBlocksGui(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    public void open(final Player player) {
        GuiSession session = GuiSession.create(player, ChatColor.DARK_AQUA + "SkyKings Buildbloecke", 27);
        set(session, 10, Material.STONE, "&7Stein");
        set(session, 11, Material.COBBLESTONE, "&7Bruchstein");
        set(session, 12, Material.WOOD, "&6Holzbretter");
        set(session, 13, Material.GLASS, "&bGlas");
        set(session, 14, Material.SANDSTONE, "&eSandstein");
        set(session, 15, Material.BRICK, "&cZiegel");
        set(session, 16, Material.QUARTZ_BLOCK, "&fQuarzblock");
        guiManager.open(session);
    }

    private void set(GuiSession session, int slot, final Material material, String name) {
        ItemStack icon = new ItemBuilder(material)
                .name(name)
                .lore("&7Klick: &f64 Bloecke erhalten", "&8Nicht verkaeuflich / kein Economy-Wert")
                .build();
        session.setItem(slot, icon, (player, event, clickedSlot) -> give(player, material, name));
    }

    private void give(Player player, Material material, String name) {
        ItemStack stack = new ItemBuilder(material, 64)
                .name(name)
                .lore(NO_SELL_LORE)
                .build();
        java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Du brauchst mindestens einen freien Inventarplatz.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "64 Buildbloecke erhalten.");
    }

    /** Fuer spaetere Shops: erkennt Items, die durch /bloecke erzeugt wurden. */
    public static boolean isNoSellBuildBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        return item.getItemMeta().getLore().contains(NO_SELL_LORE);
    }
}
