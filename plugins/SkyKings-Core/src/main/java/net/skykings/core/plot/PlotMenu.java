package net.skykings.core.plot;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

/** PlotSquared-inspiriertes Hauptmenue fuer das SkyKings-Plot-System. */
public final class PlotMenu {
    private final PlotService plots;
    private final GuiManager guiManager;

    public PlotMenu(PlotService plots) { this.plots = plots; this.guiManager = GuiManager.active(); }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Plots"), 45);
        if (!plots.hasPlot(player.getUniqueId())) {
            gui.setItem(22, UiItems.item(Material.GRASS, UiTheme.SUCCESS + "Plot automatisch claimen",
                    UiTheme.MUTED + "65x65 Baugrundstueck",
                    UiTheme.MUTED + "mit 7 Block breiten Strassen.",
                    "", UiItems.action("Klicken zum Claimen")), (p,e,s) -> {
                p.closeInventory();
                if (plots.create(p)) p.sendMessage(UiTheme.SUCCESS + "Dein Plot wurde geclaimt.");
                else p.sendMessage(UiTheme.DANGER + "Plot konnte nicht erstellt werden.");
            });
            gui.setItem(31, UiItems.item(Material.BOOK, UiTheme.PRIMARY + "Plot Verwaltung",
                    UiTheme.TEXT + "/p auto  /p h  /p visit",
                    UiTheme.TEXT + "/p add  /p trust  /p deny"));
        } else {
            PlotService.PlotData data = plots.get(player.getUniqueId());
            gui.setItem(10, UiItems.item(Material.ENDER_PEARL, UiTheme.SUCCESS + "Plot Home",
                    UiTheme.MUTED + "Teleportiere dich zu deinem Plot.", "", UiItems.action("Teleportieren")), (p,e,s) -> {
                p.closeInventory(); plots.teleportHome(p, p.getUniqueId());
            });
            gui.setItem(12, UiItems.item(Material.BED, UiTheme.MYTHIC + "Home setzen",
                    UiTheme.MUTED + "Aktuelle Position als Home."), (p,e,s) -> {
                if (plots.setHome(p.getUniqueId(), p.getLocation())) { p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.7F, 1.4F); open(p); }
                else p.sendMessage(UiTheme.DANGER + "Du musst auf deinem Plot stehen.");
            });
            gui.setItem(14, UiItems.item(Material.SKULL_ITEM, UiTheme.LEGENDARY + "Mitglieder",
                    UiTheme.MUTED + "Add: " + UiTheme.TEXT + data.getMembers().size(),
                    UiTheme.MUTED + "Trusted: " + UiTheme.TEXT + data.getTrusted().size(),
                    UiTheme.MUTED + "Denied: " + UiTheme.TEXT + data.getDenied().size(),
                    "", UiItems.action("Uebersicht oeffnen")), (p,e,s) -> openMembers(p));
            gui.setItem(16, UiItems.item(Material.REDSTONE_COMPARATOR, UiTheme.PRIMARY + "Plot Flags",
                    UiTheme.MUTED + "PvP, Explosionen und Mob-Spawns.",
                    "", UiItems.action("Einstellungen oeffnen")), (p,e,s) -> openFlags(p));
            gui.setItem(22, UiItems.item(Material.SMOOTH_BRICK, UiTheme.TEXT + "Plot #" + data.index,
                    UiTheme.MUTED + "Groesse: " + UiTheme.TEXT + "65 x 65",
                    UiTheme.MUTED + "X: " + UiTheme.TEXT + data.getMinX() + " bis " + data.getMaxX(),
                    UiTheme.MUTED + "Z: " + UiTheme.TEXT + data.getMinZ() + " bis " + data.getMaxZ(),
                    UiTheme.MUTED + "Owner: " + UiTheme.SUCCESS + player.getName()));
            gui.setItem(31, UiItems.item(Material.PAPER, UiTheme.PRIMARY + "Plot Verwaltung",
                    UiTheme.TEXT + "/p add <Spieler>",
                    UiTheme.TEXT + "/p trust <Spieler>",
                    UiTheme.TEXT + "/p deny <Spieler>",
                    UiTheme.TEXT + "/p remove <Spieler>"));
        }
        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.25F);
    }

    private void openFlags(Player player) {
        PlotService.PlotData data = plots.get(player.getUniqueId());
        if (data == null) { open(player); return; }
        GuiSession gui = GuiSession.create(player, UiTheme.title("Plot Flags"), 27);
        gui.setItem(10, flag(Material.DIAMOND_SWORD, "PvP", data.isPvp()), (p,e,s) -> { plots.setFlag(p.getUniqueId(), "pvp", !data.isPvp()); openFlags(p); });
        gui.setItem(13, flag(Material.TNT, "Explosionen", data.isExplosions()), (p,e,s) -> { plots.setFlag(p.getUniqueId(), "explosions", !data.isExplosions()); openFlags(p); });
        gui.setItem(16, flag(Material.MONSTER_EGG, "Mob-Spawns", data.isMobSpawning()), (p,e,s) -> { plots.setFlag(p.getUniqueId(), "mob-spawn", !data.isMobSpawning()); openFlags(p); });
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
    }

    private org.bukkit.inventory.ItemStack flag(Material material, String name, boolean value) {
        return UiItems.item(material, (value ? UiTheme.SUCCESS : UiTheme.DANGER) + name,
                value ? UiTheme.SUCCESS + "AN" : UiTheme.DANGER + "AUS",
                "", UiItems.action("Klicken zum Umschalten"));
    }

    private void openMembers(Player player) {
        PlotService.PlotData data = plots.get(player.getUniqueId());
        if (data == null) { open(player); return; }
        GuiSession gui = GuiSession.create(player, UiTheme.title("Plot Mitglieder"), 45);
        int slot = 9;
        slot = addSet(gui, slot, data.getTrusted(), UiTheme.SUCCESS + "Trusted", "Dauerhafte Baurechte");
        slot = addSet(gui, slot, data.getMembers(), UiTheme.PRIMARY + "Added", "Baut wenn Owner online ist");
        addSet(gui, slot, data.getDenied(), UiTheme.DANGER + "Denied", "Darf Plot nicht betreten");
        if (data.getTrusted().isEmpty() && data.getMembers().isEmpty() && data.getDenied().isEmpty()) {
            gui.setItem(22, UiItems.empty("Keine Eintraege", "/p add, /p trust oder /p deny verwenden."));
        }
        gui.setItem(36, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
    }

    private int addSet(GuiSession gui, int slot, java.util.Set<UUID> values, String category, String info) {
        for (UUID uuid : values) {
            if (slot >= 36) break;
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
            gui.setItem(slot++, UiItems.head(name, UiTheme.TEXT + name, category, UiTheme.MUTED + info,
                    UiTheme.MUTED + "Entfernen: /p remove " + name));
        }
        return slot;
    }
}
