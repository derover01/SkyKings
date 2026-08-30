package net.skykings.core.command;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Zentrales SkyKings-Befehlsmenü mit Kategorien und direkt anklickbaren Spielerbefehlen. */
public final class CommandsGui {

    private final GuiManager guiManager;

    public CommandsGui(GuiManager guiManager) { this.guiManager = guiManager; }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Commands", 27);
        gui.setItem(10, icon(Material.COMPASS, ChatColor.AQUA + "Spieler", "Grundlegende Server-Befehle"), (p,e,s) -> openPlayer(p));
        gui.setItem(12, icon(Material.DIAMOND_CHESTPLATE, ChatColor.GOLD + "Ränge & Kits", "Rangaufstieg, Kits und Übersicht"), (p,e,s) -> openRanks(p));
        gui.setItem(14, icon(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Crates", "Crates, Rewards und Öffnen"), (p,e,s) -> openCrates(p));
        gui.setItem(16, icon(Material.FEATHER, ChatColor.GREEN + "Perks", "Rang- und Gutschein-Perks"), (p,e,s) -> openPerks(p));
        if (player.hasPermission("skykings.staff.announcement") || player.hasPermission("skykings.staff.clearchat")) {
            gui.setItem(20, icon(Material.BOOK_AND_QUILL, ChatColor.BLUE + "Team", "Team- und Moderationsbefehle"), (p,e,s) -> openTeam(p));
        }
        if (player.hasPermission("skykings.admin.commands")) {
            gui.setItem(24, icon(Material.REDSTONE_TORCH_ON, ChatColor.RED + "Administration", "Nur für Admin-Berechtigte"), (p,e,s) -> openAdmin(p));
        }
        guiManager.open(gui);
    }

    private void openPlayer(Player player) {
        GuiSession gui = page(player, "Spieler Commands");
        action(gui, 9, Material.BOOK, "/commands", "Öffnet dieses Befehlsmenü.", "commands");
        action(gui, 10, Material.GOLD_INGOT, "/top", "Öffnet die PvP-Leaderboards für Kills, Beststreak und K/D.", "top");
        action(gui, 11, Material.DIAMOND_SWORD, "/stats", "Zeigt deine PvP-Stats, Streak und Beststreak.", "stats");
        action(gui, 12, Material.BLAZE_POWDER, "/killeffect", "Wählt deinen freigeschalteten kosmetischen Kill-Effect.", "killeffect");
        action(gui, 13, Material.CHEST, "/kit", "Öffnet deine verfügbaren Rang-Kits.", "kit");
        action(gui, 15, Material.EMERALD, "/ränge", "Zeigt alle Ränge und deinen Fortschritt.", "raenge");
        action(gui, 17, Material.LAVA_BUCKET, "/trash / /müll", "Öffnet ein 6x9-Müllinventar. Inhalt wird beim Schließen gelöscht.", "trash");
        back(gui);
        guiManager.open(gui);
    }

    private void openRanks(Player player) {
        GuiSession gui = page(player, "Ränge & Kits");
        action(gui, 11, Material.EXP_BOTTLE, "/rankup", "Kauft den nächsten Free-Rang mit Coins.", "rankup");
        action(gui, 13, Material.CHEST, "/kit", "Öffnet die Kit-Auswahl.", "kit");
        action(gui, 15, Material.DIAMOND, "/ränge", "Öffnet die komplette Rang-Übersicht.", "raenge");
        back(gui);
        guiManager.open(gui);
    }

    private void openCrates(Player player) {
        GuiSession gui = page(player, "Crates");
        gui.setItem(11, icon(Material.SKULL_ITEM, ChatColor.GOLD + "Crate-Item", ChatColor.GRAY + "Linksklick: Rewards ansehen", ChatColor.GRAY + "Rechtsklick: Öffnungsmenü", ChatColor.GRAY + "Animation oder Sofortgewinn auswählbar"));
        action(gui, 15, Material.NETHER_STAR, "/craterewards", "Paid-Rank-Rewards mit eigenen Cooldowns.", "craterewards");
        back(gui);
        guiManager.open(gui);
    }

    private void openPerks(Player player) {
        GuiSession gui = page(player, "Perks");
        action(gui, 9, Material.FEATHER, "/fly", "Flugmodus für Knight+ oder Fly-Recht.", "fly");
        action(gui, 11, Material.CHEST, "/stack", "Stackt gleiche Inventar-Items.", "stack");
        action(gui, 13, Material.BRICK, "/blöcke", "Öffnet den großen kostenlosen Baublock-Katalog.", "bloecke");
        action(gui, 15, Material.ANVIL, "/repair", "Repariert das Item in deiner Hand.", "repair");
        action(gui, 17, Material.ENDER_CHEST, "/ec", "Öffnet die eigene Enderchest ab Gold.", "ec");
        back(gui);
        guiManager.open(gui);
    }

    private void openTeam(Player player) {
        GuiSession gui = page(player, "Team Commands");
        if (player.hasPermission("skykings.staff.announcement")) gui.setItem(11, infoCommand(Material.PAPER, "/announcement <Nachricht>", "Sendet eine auffällige Server-Ankündigung."));
        if (player.hasPermission("skykings.staff.clearchat")) action(gui, 13, Material.EMPTY_MAP, "/clearchat / /cc", "Leert den Chat für alle Spieler.", "clearchat");
        if (player.hasPermission("skykings.staff.gamemode")) gui.setItem(15, infoCommand(Material.GRASS, "/gm <0|1|2|3> [Spieler]", "Ändert den Gamemode."));
        back(gui);
        guiManager.open(gui);
    }

    private void openAdmin(Player player) {
        GuiSession gui = page(player, "Admin Commands");
        gui.setItem(9, infoCommand(Material.NAME_TAG, "/rang <Spieler> <Rang>", "Setzt den SkyKings-Rang eines Online-Spielers."));
        gui.setItem(10, infoCommand(Material.PAPER, "/rechte <Spieler> <Recht>", "Vergibt ein freigegebenes Feature-Recht."));
        gui.setItem(11, infoCommand(Material.CHEST, "/crate give <Spieler> <Typ> [Anzahl]", "Gibt stackbare Crate-Batches aus."));
        action(gui, 12, Material.EMERALD, "/gutscheine", "Öffnet die Gutschein-Erzeugung.", "gutscheine");
        gui.setItem(14, infoCommand(Material.GRASS, "/gm <0|1|2|3> [Spieler]", "Gamemode mit Staff-Recht."));
        gui.setItem(16, icon(Material.SIGN, ChatColor.GREEN + "Free Sign erstellen", ChatColor.GRAY + "Zeile 1: " + ChatColor.WHITE + "[FREE]", ChatColor.GRAY + "Zeile 2: " + ChatColor.WHITE + "Item-ID, z. B. 276 oder 5:2", ChatColor.GRAY + "Zeile 3: " + ChatColor.WHITE + "Menge", ChatColor.DARK_GRAY + "XP-Flaschen: 384 • Lapis: 351:4"));
        back(gui);
        guiManager.open(gui);
    }

    private void action(GuiSession gui, int slot, Material material, String shownCommand, String description, String command) {
        gui.setItem(slot, command(material, shownCommand, description), (p,e,s) -> run(p, command));
    }

    private GuiSession page(Player player, String title) { return GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | " + title, 27); }
    private void back(GuiSession gui) { gui.setItem(22, icon(Material.ARROW, ChatColor.YELLOW + "Zurück", "Zur Hauptübersicht"), (p,e,s) -> open(p)); }
    private void run(Player player, String command) { player.closeInventory(); player.performCommand(command); }
    private ItemStack command(Material material, String command, String description) { return icon(material, ChatColor.WHITE + command, ChatColor.GRAY + description, "", ChatColor.YELLOW + "Klicken zum Ausführen"); }
    private ItemStack infoCommand(Material material, String command, String description) { return icon(material, ChatColor.WHITE + command, ChatColor.GRAY + description, "", ChatColor.DARK_GRAY + "Befehl mit Argumenten"); }
    private ItemStack icon(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<String>();
        lore.addAll(Arrays.asList(loreLines));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
