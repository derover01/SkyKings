package net.skykings.core.plot;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.UUID;

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
                    open(p);
                } else {
                    p.sendMessage(ChatColor.RED + "Du musst auf deinem eigenen Plot stehen.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
                }
            });
            gui.setItem(14, item(Material.SKULL_ITEM, ChatColor.GOLD.toString() + ChatColor.BOLD + "MITGLIEDER / TRUST",
                    ChatColor.GRAY + "Trusted: " + ChatColor.WHITE + data.getTrusted().size(), "",
                    ChatColor.YELLOW + "Klicken zum Verwalten",
                    ChatColor.DARK_GRAY + "Hinzufuegen: /p trust <Spieler>"), (p,e,s) -> openTrusted(p));
            gui.setItem(16, item(Material.COMPASS, ChatColor.AQUA.toString() + ChatColor.BOLD + "PLOT BESUCHEN",
                    ChatColor.GRAY + "Besuche den Plot eines anderen Spielers.", "",
                    ChatColor.YELLOW + "/p visit <Spieler>"));
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

    private void openTrusted(Player player) {
        PlotService.PlotData data = plots.get(player.getUniqueId());
        if (data == null) { open(player); return; }
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "Plot " + ChatColor.GRAY + "| " + ChatColor.GOLD + "Trust", 45);
        ItemStack filler = pane((short) 15, " ");
        for (int i = 0; i < 45; i++) gui.setItem(i, filler);
        gui.setItem(4, item(Material.SKULL_ITEM, ChatColor.GOLD.toString() + ChatColor.BOLD + "PLOT TRUST",
                ChatColor.GRAY + "Spieler mit Baurechten auf deinem Plot.",
                ChatColor.GRAY + "Hinzufuegen mit " + ChatColor.AQUA + "/p trust <Spieler>"));
        int slot = 10;
        for (UUID uuid : data.getTrusted()) {
            if (slot > 34) break;
            final UUID target = uuid;
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
            gui.setItem(slot++, skull(name, ChatColor.YELLOW + name,
                    offline.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.GRAY + "Offline",
                    "",
                    ChatColor.RED + "Klicken, um Trust zu entfernen"), (p,e,s) -> {
                if (plots.untrust(p.getUniqueId(), target)) {
                    p.sendMessage(ChatColor.YELLOW + "Plot-Trust entfernt.");
                    p.playSound(p.getLocation(), Sound.CLICK, 0.6F, 0.8F);
                }
                openTrusted(p);
            });
        }
        if (data.getTrusted().isEmpty()) {
            gui.setItem(22, item(Material.BARRIER, ChatColor.RED + "Noch niemand vertraut",
                    ChatColor.GRAY + "Nutze " + ChatColor.AQUA + "/p trust <Spieler>",
                    ChatColor.GRAY + "um einem Freund Baurechte zu geben."));
        }
        gui.setItem(40, item(Material.ARROW, ChatColor.YELLOW + "Zurueck", ChatColor.GRAY + "Zur Plot-Uebersicht"), (p,e,s) -> open(p));
        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CLICK, 0.55F, 1.35F);
    }

    private void decorate(GuiSession gui) {
        ItemStack gray = pane((short) 15, " ");
        ItemStack green = pane((short) 5, ChatColor.GREEN + "SkyKings Plots");
        for (int i = 0; i < 45; i++) if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, gray);
        gui.setItem(4, green); gui.setItem(40, green);
    }

    private ItemStack skull(String owner, String name, String... lore) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(owner);
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
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
