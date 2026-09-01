package net.skykings.core.plot;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

/** PlotSquared-inspiriertes Hauptmenue fuer das SkyKings-Plot-System. */
public final class PlotMenu {
    private final PlotService plots;
    private final PlotBorderService borders;
    private final GuiManager guiManager;
    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.GERMANY);

    public PlotMenu(PlotService plots, PlotBorderService borders) {
        this.plots = plots;
        this.borders = borders;
        this.guiManager = GuiManager.active();
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Plots"), 45);
        if (!plots.hasPlot(player.getUniqueId())) {
            gui.setItem(22, UiItems.item(Material.GRASS, UiTheme.SUCCESS + "Plot automatisch claimen",
                    UiTheme.MUTED + "65x65 Baugrundstueck",
                    UiTheme.MUTED + "mit neutralen Stone-Brick-Wegen.",
                    "", UiItems.action("Klicken zum Claimen")), (p,e,s) -> {
                p.closeInventory();
                if (plots.create(p)) p.sendMessage(UiTheme.SUCCESS + "Dein Plot wurde geclaimt.");
                else p.sendMessage(UiTheme.DANGER + "Plot konnte nicht erstellt werden.");
            });
            gui.setItem(31, UiItems.item(Material.BOOK, UiTheme.PRIMARY + "Plot Verwaltung",
                    UiTheme.TEXT + "/p auto  /p h  /p visit",
                    UiTheme.TEXT + "/p add  /p trust  /p deny",
                    UiTheme.TEXT + "/p flags  /p rand"));
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
                    UiTheme.MUTED + "PvP, Explosionen, Feuer",
                    UiTheme.MUTED + "und Mob-Spawns.",
                    "", UiItems.action("Einstellungen oeffnen")), (p,e,s) -> openFlags(p));
            gui.setItem(22, UiItems.item(Material.GRASS, UiTheme.TEXT + "Plot #" + data.index,
                    UiTheme.MUTED + "Claim: " + UiTheme.TEXT + "65 x 65",
                    UiTheme.MUTED + "Nur die Flaeche innerhalb",
                    UiTheme.MUTED + "der Stone-Brick-Wege gehoert dir.",
                    UiTheme.MUTED + "Owner: " + UiTheme.SUCCESS + player.getName()));
            gui.setItem(30, UiItems.item(borders.selected(player.getUniqueId()).getMaterial(), UiTheme.LEGENDARY + "Plot-Rand",
                    UiTheme.MUTED + "Aktuell: " + UiTheme.TEXT + borders.selected(player.getUniqueId()).getDisplayName(),
                    UiTheme.MUTED + "Raender mit Coins freischalten.",
                    "", UiItems.action("Rand-Shop oeffnen")), (p,e,s) -> openBorders(p));
            gui.setItem(32, UiItems.item(Material.PAPER, UiTheme.PRIMARY + "Plot Verwaltung",
                    UiTheme.TEXT + "/p add <Spieler>",
                    UiTheme.TEXT + "/p trust <Spieler>",
                    UiTheme.TEXT + "/p deny / undeny <Spieler>",
                    UiTheme.TEXT + "/p flags  /p rand"));
        }
        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.25F);
    }

    public void openBorders(Player player) {
        if (!plots.hasPlot(player.getUniqueId())) { open(player); return; }
        GuiSession gui = GuiSession.create(player, UiTheme.title("Plot-Rand"), 45);
        PlotBorderTheme selected = borders.selected(player.getUniqueId());
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 21, 23};
        PlotBorderTheme[] themes = PlotBorderTheme.values();
        for (int i = 0; i < themes.length && i < slots.length; i++) {
            final PlotBorderTheme theme = themes[i];
            boolean owned = borders.owns(player.getUniqueId(), theme);
            boolean active = selected == theme;
            String status = active ? UiTheme.SUCCESS + "ACTIVE"
                    : owned ? UiTheme.PRIMARY + "FREIGESCHALTET"
                    : UiTheme.WARNING + numbers.format(theme.getPrice()) + " Coins";
            gui.setItem(slots[i], UiItems.item(theme.getMaterial(),
                    (active ? UiTheme.SUCCESS : UiTheme.TEXT) + theme.getDisplayName(),
                    status,
                    active ? UiTheme.MUTED + "Dieser Rand ist aktiv."
                            : owned ? UiItems.action("Klicken zum Auswaehlen")
                            : UiItems.action("Klicken zum Kaufen")), (p,e,s) -> {
                if (borders.purchaseAndSelect(p, theme)) {
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.65F, 1.35F);
                    p.sendMessage(UiTheme.SUCCESS + "Plot-Rand aktiviert: " + ChatColor.WHITE + theme.getDisplayName());
                    openBorders(p);
                } else {
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 0.7F, 1.0F);
                    p.sendMessage(UiTheme.DANGER + "Dafuer fehlen dir Coins.");
                }
            });
        }
        gui.setItem(31, UiItems.item(Material.GOLD_NUGGET, UiTheme.LEGENDARY + "Deine Coins",
                UiTheme.TEXT + numbers.format(borders.balance(player.getUniqueId())) + " Coins"));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.3F);
    }

    public void openFlags(Player player) {
        PlotService.PlotData data = plots.get(player.getUniqueId());
        if (data == null) { open(player); return; }
        GuiSession gui = GuiSession.create(player, UiTheme.title("Plot Flags"), 27);
        gui.setItem(10, flag(Material.DIAMOND_SWORD, "PvP", "Spielerduelle auf deinem Plot.", data.isPvp()),
                (p,e,s) -> { plots.setFlag(p.getUniqueId(), "pvp", !data.isPvp()); openFlags(p); });
        gui.setItem(12, flag(Material.TNT, "Explosionen", "TNT und Entity-Explosionen.", data.isExplosions()),
                (p,e,s) -> { plots.setFlag(p.getUniqueId(), "explosions", !data.isExplosions()); openFlags(p); });
        gui.setItem(14, flag(Material.FLINT_AND_STEEL, "Feuer", "Entzuenden und Abbrennen erlauben.", data.isFire()),
                (p,e,s) -> { plots.setFlag(p.getUniqueId(), "fire", !data.isFire()); openFlags(p); });
        gui.setItem(16, flag(Material.MONSTER_EGG, "Mob-Spawns", "Natuerliche Kreaturen auf dem Plot.", data.isMobSpawning()),
                (p,e,s) -> { plots.setFlag(p.getUniqueId(), "mob-spawn", !data.isMobSpawning()); openFlags(p); });
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
    }

    private org.bukkit.inventory.ItemStack flag(Material material, String name, String description, boolean value) {
        return UiItems.item(material, (value ? UiTheme.SUCCESS : UiTheme.TEXT) + name,
                UiTheme.MUTED + description,
                value ? UiTheme.SUCCESS + "ACTIVE" : UiTheme.DISABLED + "DISABLED",
                "", UiItems.action("Klicken zum Umschalten"));
    }

    private void openMembers(Player player) {
        PlotService.PlotData data = plots.get(player.getUniqueId());
        if (data == null) { open(player); return; }
        GuiSession gui = GuiSession.create(player, UiTheme.title("Plot Mitglieder"), 45);
        int slot = 9;
        slot = addSet(gui, slot, data.getTrusted(), UiTheme.SUCCESS + "Trusted", "Dauerhafte Baurechte", false);
        slot = addSet(gui, slot, data.getMembers(), UiTheme.PRIMARY + "Added", "Baut wenn Owner online ist", false);
        addSet(gui, slot, data.getDenied(), UiTheme.DANGER + "Denied", "Darf Plot nicht betreten", true);
        if (data.getTrusted().isEmpty() && data.getMembers().isEmpty() && data.getDenied().isEmpty()) {
            gui.setItem(22, UiItems.empty("Keine Eintraege", "/p add, /p trust oder /p deny verwenden."));
        }
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
    }

    private int addSet(GuiSession gui, int slot, java.util.Set<UUID> values, String category, String info, boolean denied) {
        for (UUID uuid : values) {
            if (slot >= 36) break;
            final UUID target = uuid;
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
            gui.setItem(slot++, UiItems.head(name, UiTheme.TEXT + name, category, UiTheme.MUTED + info,
                    "", denied ? UiTheme.DANGER + "Klicken: Deny entfernen" : UiTheme.MUTED + "Klicken: Rechte entfernen"), (p,e,s) -> {
                boolean changed = denied ? plots.undeny(p.getUniqueId(), target) : plots.remove(p.getUniqueId(), target);
                p.playSound(p.getLocation(), changed ? Sound.CLICK : Sound.VILLAGER_NO, 0.6F, changed ? 1.25F : 1.0F);
                openMembers(p);
            });
        }
        return slot;
    }
}
