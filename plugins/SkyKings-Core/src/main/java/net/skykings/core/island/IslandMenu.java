package net.skykings.core.island;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
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
import java.util.List;
import java.util.UUID;

/** Hochwertiges /is Hauptmenue fuer den Vanilla-1.8.9-Client. */
public final class IslandMenu {
    private final GuiManager guiManager;
    private final IslandService islands;

    public IslandMenu(GuiManager guiManager, IslandService islands) {
        this.guiManager = guiManager;
        this.islands = islands;
    }

    public void open(Player player) {
        boolean hasIsland = islands.hasIsland(player.getUniqueId());
        GuiSession gui = GuiSession.create(player, UiTheme.title("Island"), 45);
        decorate(gui);

        if (!hasIsland) {
            gui.setItem(22, UiItems.item(Material.GRASS, UiTheme.SUCCESS + "Insel erstellen",
                    UiTheme.MUTED + "Klassische SkyBlock-Starterinsel",
                    UiTheme.MUTED + "mit Baum und Ressourcen-Truhe.",
                    "",
                    UiTheme.TEXT + "Schutz: 129 x 129",
                    UiItems.action("Klicken zum Erstellen")), (p,e,s) -> {
                p.closeInventory();
                if (islands.create(p)) p.sendMessage(UiTheme.SUCCESS + "Deine SkyKings-Insel wurde erstellt.");
                else p.sendMessage(UiTheme.DANGER + "Deine Insel konnte nicht erstellt werden.");
            });
            gui.setItem(31, UiItems.item(Material.BOOK, UiTheme.PRIMARY + "Island-System",
                    UiTheme.MUTED + "Baue deine Insel aus.",
                    UiTheme.MUTED + "Wertvolle Bloecke erhoehen dein Level.",
                    UiTheme.MUTED + "[Welcome] aktiviert Besuche."));
            gui.setItem(40, UiItems.item(Material.DIAMOND, UiTheme.LEGENDARY + "Island Top 10",
                    UiTheme.MUTED + "Die wertvollsten Inseln.", "", UiItems.action("Oeffnen")), (p,e,s) -> openTop(p));
        } else {
            IslandService.IslandData data = islands.get(player.getUniqueId());
            gui.setItem(10, UiItems.item(Material.ENDER_PEARL, UiTheme.PRIMARY + "Island Home",
                    UiTheme.MUTED + "Teleport zu deinem Island-Home.", "", UiItems.action("Teleportieren")), (p,e,s) -> {
                p.closeInventory(); islands.teleportHome(p, p.getUniqueId());
            });
            gui.setItem(12, UiItems.item(Material.BED, UiTheme.MYTHIC + "Home setzen",
                    UiTheme.MUTED + "Aktuelle Position speichern."), (p,e,s) -> {
                if (islands.setHome(p.getUniqueId(), p.getLocation())) {
                    p.sendMessage(UiTheme.SUCCESS + "Island-Home gesetzt.");
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.7F, 1.4F); open(p);
                } else {
                    p.sendMessage(UiTheme.DANGER + "Du musst auf deiner Insel stehen.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F);
                }
            });
            boolean welcome = islands.hasWelcome(player.getUniqueId());
            gui.setItem(14, UiItems.item(Material.SIGN, (welcome ? UiTheme.SUCCESS : UiTheme.DANGER) + "Besucher-Punkt",
                    welcome ? UiTheme.SUCCESS + "Oeffentlich" : UiTheme.DANGER + "Privat",
                    UiTheme.MUTED + "[Welcome] auf ein Schild schreiben.",
                    UiTheme.MUTED + "Besucher landen direkt dort."));
            gui.setItem(16, UiItems.item(Material.SKULL_ITEM, UiTheme.LEGENDARY + "Vertraute Spieler",
                    UiTheme.MUTED + "Trusted: " + UiTheme.TEXT + data.getTrusted().size(), "", UiItems.action("Verwalten")),
                    (p,e,s) -> openTrusted(p));
            gui.setItem(22, UiItems.item(Material.GRASS, UiTheme.TEXT + "Island #" + data.index,
                    UiTheme.MUTED + "Level: " + UiTheme.PRIMARY + UiFormat.number(data.getLevel()),
                    UiTheme.MUTED + "Levelpunkte: " + UiTheme.TEXT + UiFormat.number(data.getLevelPoints()),
                    UiTheme.MUTED + "Schutz: " + UiTheme.TEXT + "129 x 129",
                    UiTheme.MUTED + "X: " + UiTheme.TEXT + data.getMinX() + " bis " + data.getMaxX(),
                    UiTheme.MUTED + "Z: " + UiTheme.TEXT + data.getMinZ() + " bis " + data.getMaxZ()));
            gui.setItem(30, UiItems.item(Material.PAPER, UiTheme.PRIMARY + "Schnellbefehle",
                    UiTheme.TEXT + "/is visit <Spieler>",
                    UiTheme.TEXT + "/is trust <Spieler>",
                    UiTheme.TEXT + "/is top"));
            gui.setItem(32, UiItems.item(Material.DIAMOND, UiTheme.LEGENDARY + "Island Top 10",
                    UiTheme.MUTED + "Vergleiche Island-Level.", "", UiItems.action("Oeffnen")), (p,e,s) -> openTop(p));
        }

        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.35F);
    }

    public void openTop(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Island Top 10"), 45);
        List<IslandService.IslandData> top = islands.top(10);
        int[] slots = {10,11,12,13,14,15,16,20,22,24};
        for (int i = 0; i < top.size() && i < slots.length; i++) {
            IslandService.IslandData data = top.get(i);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(data.owner);
            String name = owner.getName() == null ? data.owner.toString().substring(0, 8) : owner.getName();
            final UUID ownerId = data.owner;
            boolean publicIsland = islands.hasWelcome(ownerId);
            gui.setItem(slots[i], UiItems.head(name,
                    (i == 0 ? UiTheme.LEGENDARY : UiTheme.TEXT) + "#" + (i + 1) + " " + name,
                    UiTheme.MUTED + "Island-Level " + UiTheme.PRIMARY + UiFormat.number(data.getLevel()),
                    UiTheme.MUTED + "Punkte " + UiTheme.TEXT + UiFormat.number(data.getLevelPoints()),
                    publicIsland ? UiTheme.SUCCESS + "Besuch moeglich" : UiTheme.DISABLED + "Privat",
                    "",
                    publicIsland ? UiItems.action("Klicken zum Besuchen") : UiTheme.DISABLED + "Kein [Welcome]"), (p,e,s) -> {
                if (islands.hasWelcome(ownerId)) { p.closeInventory(); islands.visit(p, ownerId); }
            });
        }
        if (top.isEmpty()) gui.setItem(22, UiItems.empty("Noch keine Inseln", "Erstelle die erste SkyKings-Insel."));
        gui.setItem(36, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CLICK, 0.55F, 1.35F);
    }

    private void openTrusted(Player player) {
        IslandService.IslandData data = islands.get(player.getUniqueId());
        if (data == null) { open(player); return; }
        GuiSession gui = GuiSession.create(player, UiTheme.title("Island Trust"), 45);
        int slot = 10;
        for (UUID uuid : data.getTrusted()) {
            if (slot > 34) break;
            final UUID target = uuid;
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
            gui.setItem(slot++, skull(name, UiTheme.TEXT + name,
                    offline.isOnline() ? UiTheme.SUCCESS + "Online" : UiTheme.MUTED + "Offline",
                    "", UiTheme.DANGER + "Klicken: Trust entfernen"), (p,e,s) -> {
                if (islands.untrust(p.getUniqueId(), target)) p.playSound(p.getLocation(), Sound.CLICK, 0.6F, 0.8F);
                openTrusted(p);
            });
        }
        if (data.getTrusted().isEmpty()) gui.setItem(22, UiItems.empty("Niemand vertraut", "/is trust <Spieler> zum Hinzufuegen."));
        gui.setItem(36, UiItems.back(), (p,e,s) -> open(p));
        guiManager.open(gui);
    }

    private void decorate(GuiSession gui) {
        ItemStack dark = pane((short) 15, " ");
        for (int i = 0; i < 45; i++) if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, dark);
    }

    private ItemStack skull(String owner, String name, String... lore) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta(); meta.setOwner(owner); meta.setDisplayName(name); meta.setLore(Arrays.asList(lore)); item.setItemMeta(meta); return item;
    }
    private ItemStack pane(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); item.setItemMeta(meta); return item;
    }
}
