package net.skykings.core.plot;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** PlotSquared-inspiriertes Hauptmenue fuer das eigene SkyKings-Plot-System. */
public final class PlotMenu {
    private final PlotService plots;
    private final GuiManager guiManager;

    public PlotMenu(PlotService plots) {
        this.plots = plots;
        this.guiManager = GuiManager.active();
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings " + ChatColor.GRAY + "| " + ChatColor.GREEN + "Plots", 45);
        decorate(gui);
        if (!plots.hasPlot(player.getUniqueId())) {
            gui.setItem(22, item(Material.GRASS, ChatColor.GREEN.toString() + ChatColor.BOLD + "PLOT AUTOMATISCH CLAIMEN",
                    ChatColor.GRAY + "Wie /plot auto bei klassischen Plotservern.", "",
                    ChatColor.WHITE + "Groesse: " + ChatColor.AQUA + "65x65",
                    ChatColor.WHITE + "Welt: " + ChatColor.AQUA + PlotService.WORLD_NAME,
                    "", ChatColor.GREEN + "Klicken zum Claimen"), (p,e,s) -> {
                p.closeInventory();
                if (plots.create(p)) {
                    p.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "PLOT GECLAIMT! " + ChatColor.GRAY + "Dein neuer Plot ist bereit.");
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.8F, 1.25F);
                } else p.sendMessage(ChatColor.RED + "Plot konnte nicht erstellt werden.");
            });
            gui.setItem(31, item(Material.BOOK, ChatColor.AQUA + "Plot-Befehle",
                    ChatColor.YELLOW + "/plot auto " + ChatColor.GRAY + "Plot claimen",
                    ChatColor.YELLOW + "/plot h " + ChatColor.GRAY + "Plot-Home",
                    ChatColor.YELLOW + "/plot visit <Spieler>",
                    ChatColor.YELLOW + "/plot trust <Spieler>"));
        } else {
            PlotService.PlotData data = plots.get(player.getUniqueId());
            gui.setItem(10, item(Material.ENDER_PEARL, ChatColor.GREEN.toString() + ChatColor.BOLD + "PLOT HOME",
                    ChatColor.GRAY + "Teleportiere dich zu deinem Plot.", "", ChatColor.YELLOW + "Klicken"), (p,e,s) -> {
                p.closeInventory(); plots.teleportHome(p, p.getUniqueId());
            });
            gui.setItem(12, item(Material.BED, ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "HOME SETZEN",
                    ChatColor.GRAY + "Setzt die aktuelle Position als Plot-Home."), (p,e,s) -> {
                if (plots.setHome(p.getUniqueId(), p.getLocation())) {
                    p.sendMessage(ChatColor.GREEN + "Plot-Home gesetzt.");
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.7F, 1.4F);
                } else {
                    p.sendMessage(ChatColor.RED + "Du musst auf deinem eigenen Plot stehen.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
                }
            });
            gui.setItem(14, item(Material.SKULL_ITEM, ChatColor.GOLD.toString() + ChatColor.BOLD + "MITGLIEDER / TRUST",
                    ChatColor.GRAY + "Trusted: " + ChatColor.WHITE + data.getTrusted().size(), "",
                    ChatColor.YELLOW + "/plot trust <Spieler>",
                    ChatColor.YELLOW + "/plot untrust <Spieler>"));
            gui.setItem(16, item(Material.COMPASS, ChatColor.AQUA.toString() + ChatColor.BOLD + "PLOT BESUCHEN",
                    ChatColor.GRAY + "Besuche den Plot eines anderen Spielers.", "",
                    ChatColor.YELLOW + "/plot visit <Spieler>"));
            gui.setItem(22, item(Material.SMOOTH_BRICK, ChatColor.WHITE.toString() + ChatColor.BOLD + "PLOT #" + data.index,
                    ChatColor.GRAY + "Groesse: " + ChatColor.WHITE + "65x65",
                    ChatColor.GRAY + "Center: " + ChatColor.WHITE + data.centerX + ", " + data.centerZ,
                    ChatColor.GRAY + "Owner: " + ChatColor.GREEN + player.getName()));
            gui.setItem(31, item(Material.PAPER, ChatColor.YELLOW.toString() + ChatColor.BOLD + "PLOTSQUARED-STYLE SHORTCUTS",
                    ChatColor.AQUA + "/p h" + ChatColor.GRAY + " - Home",
                    ChatColor.AQUA + "/p v <Spieler>" + ChatColor.GRAY + " - Visit",
                    ChatColor.AQUA + "/p trust <Spieler>" + ChatColor.GRAY + " - Trust",
                    ChatColor.AQUA + "/p info" + ChatColor.GRAY + " - Info"));
        }
        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.25F);
    }

    private void decorate(GuiSession gui) {
        ItemStack gray = pane((short) 15, " ");
        ItemStack green = pane((short) 5, ChatColor.GREEN + "SkyKings Plots");
        for (int i = 0; i < 45; i++) if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, gray);
        gui.setItem(4, green); gui.setItem(40, green);
    }

    private ItemStack pane(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); item.setItemMeta(meta); return item;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); return item;
    }
}
