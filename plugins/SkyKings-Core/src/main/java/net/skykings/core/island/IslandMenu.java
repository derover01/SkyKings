package net.skykings.core.island;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings " + ChatColor.GRAY + "| " + ChatColor.AQUA + "Island", 45);
        decorate(gui);

        if (!hasIsland) {
            gui.setItem(22, item(Material.GRASS, ChatColor.GREEN.toString() + ChatColor.BOLD + "DEINE INSEL ERSTELLEN",
                    ChatColor.GRAY + "Starte dein eigenes SkyKings Island.",
                    "",
                    ChatColor.YELLOW + "• Starterinsel mit Ressourcen",
                    ChatColor.YELLOW + "• Privater 129x129 Bereich",
                    ChatColor.YELLOW + "• Trust- und Welcome-System",
                    "",
                    ChatColor.GREEN + "Klicken zum Erstellen"), (p,e,s) -> {
                p.closeInventory();
                if (islands.create(p)) {
                    p.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS ISLANDS " + ChatColor.GREEN + "Deine Insel wurde erstellt!");
                } else {
                    p.sendMessage(ChatColor.RED + "Deine Insel konnte nicht erstellt werden.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 0.7F, 1.0F);
                }
            });
            gui.setItem(31, item(Material.BOOK, ChatColor.AQUA + "Wie funktioniert Islands?",
                    ChatColor.GRAY + "Baue, vertraue Freunden und entscheide selbst,",
                    ChatColor.GRAY + "ob Besucher deine Insel betreten duerfen.",
                    "",
                    ChatColor.WHITE + "Besuche werden erst durch ein " + ChatColor.GREEN + "[Welcome]" + ChatColor.WHITE + "-Schild aktiviert."));
        } else {
            IslandService.IslandData data = islands.get(player.getUniqueId());
            gui.setItem(10, item(Material.ENDER_PEARL, ChatColor.AQUA.toString() + ChatColor.BOLD + "ZU DEINER INSEL",
                    ChatColor.GRAY + "Teleportiert dich zu deinem Island-Home.", "", ChatColor.YELLOW + "Klicken zum Teleportieren"), (p,e,s) -> {
                p.closeInventory();
                islands.teleportHome(p, p.getUniqueId());
            });
            gui.setItem(12, item(Material.BED, ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "HOME SETZEN",
                    ChatColor.GRAY + "Setzt deine aktuelle Position als Island-Home.", "", ChatColor.YELLOW + "Du musst auf deiner Insel stehen."), (p,e,s) -> {
                if (islands.setHome(p.getUniqueId(), p.getLocation())) {
                    p.sendMessage(ChatColor.GREEN + "Island-Home erfolgreich gesetzt.");
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.7F, 1.4F);
                    open(p);
                } else {
                    p.sendMessage(ChatColor.RED + "Du musst dich auf deiner eigenen Insel befinden.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 0.7F, 1.0F);
                }
            });
            boolean welcome = islands.hasWelcome(player.getUniqueId());
            gui.setItem(14, item(Material.SIGN, (welcome ? ChatColor.GREEN : ChatColor.RED).toString() + ChatColor.BOLD + "BESUCHER-PUNKT",
                    welcome ? ChatColor.GREEN + "Deine Insel ist aktuell oeffentlich." : ChatColor.RED + "Deine Insel ist aktuell privat.",
                    "",
                    ChatColor.GRAY + "Platziere auf deiner Insel ein Schild mit:",
                    ChatColor.WHITE + "[Welcome]",
                    "",
                    ChatColor.GRAY + "Besucher landen direkt an diesem Schild."));
            gui.setItem(16, item(Material.SKULL_ITEM, ChatColor.GOLD.toString() + ChatColor.BOLD + "VERTRAUTE SPIELER",
                    ChatColor.GRAY + "Trusted Spieler: " + ChatColor.WHITE + data.getTrusted().size(),
                    "",
                    ChatColor.YELLOW + "/is trust <Spieler>",
                    ChatColor.YELLOW + "/is untrust <Spieler>"));
            gui.setItem(22, item(Material.CHEST, ChatColor.YELLOW.toString() + ChatColor.BOLD + "ISLAND INFO",
                    ChatColor.GRAY + "Groesse: " + ChatColor.WHITE + "129x129",
                    ChatColor.GRAY + "Island-ID: " + ChatColor.WHITE + "#" + data.index,
                    ChatColor.GRAY + "Center: " + ChatColor.WHITE + data.centerX + ", " + data.centerZ,
                    ChatColor.GRAY + "Besuch: " + (welcome ? ChatColor.GREEN + "OFFEN" : ChatColor.RED + "PRIVAT"),
                    "",
                    ChatColor.DARK_GRAY + "Deine Startertruhe steht auf der Startinsel."));
            gui.setItem(30, item(Material.PAPER, ChatColor.WHITE.toString() + ChatColor.BOLD + "SCHNELLBEFEHLE",
                    ChatColor.AQUA + "/is visit <Spieler> " + ChatColor.GRAY + "Besuchen",
                    ChatColor.AQUA + "/is trust <Spieler> " + ChatColor.GRAY + "Vertrauen",
                    ChatColor.AQUA + "/is sethome " + ChatColor.GRAY + "Home setzen"));
            gui.setItem(32, item(Material.DIAMOND, ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS ISLAND",
                    ChatColor.GRAY + "Dein eigener Bereich im Koenigreich.",
                    ChatColor.GRAY + "Baue ihn aus und mach ihn besuchenswert."));
        }

        guiManager.open(gui);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.45F, 1.35F);
    }

    private void decorate(GuiSession gui) {
        ItemStack dark = pane((short) 15, " ");
        ItemStack cyan = pane((short) 9, ChatColor.AQUA + "SkyKings");
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, dark);
        }
        gui.setItem(4, cyan);
        gui.setItem(40, cyan);
    }

    private ItemStack pane(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lines = new ArrayList<String>();
        if (lore != null) lines.addAll(Arrays.asList(lore));
        meta.setLore(lines);
        item.setItemMeta(meta);
        return item;
    }
}
