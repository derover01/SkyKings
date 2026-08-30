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

/** Zentrales SkyKings-Befehlsmenue mit Kategorien und direkt anklickbaren Spielerbefehlen. */
public final class CommandsGui {

    private final GuiManager guiManager;

    public CommandsGui(GuiManager guiManager) { this.guiManager = guiManager; }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Commands", 27);
        gui.setItem(10, icon(Material.COMPASS, ChatColor.AQUA + "Spieler", "Grundlegende Server-Befehle"),
                (p, e, s) -> openPlayer(p));
        gui.setItem(12, icon(Material.DIAMOND_CHESTPLATE, ChatColor.GOLD + "Raenge & Kits", "Rangaufstieg, Kits und Uebersicht"),
                (p, e, s) -> openRanks(p));
        gui.setItem(14, icon(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Crates", "Crates, Rewards und Oeffnen"),
                (p, e, s) -> openCrates(p));
        gui.setItem(16, icon(Material.FEATHER, ChatColor.GREEN + "Perks", "Rang- und Gutschein-Perks"),
                (p, e, s) -> openPerks(p));
        if (player.isOp() || player.hasPermission("skykings.admin.commands")) {
            gui.setItem(22, icon(Material.REDSTONE_TORCH_ON, ChatColor.RED + "Administration",
                    "Rang-, Rechte-, Crate- und Gutschein-Verwaltung"), (p, e, s) -> openAdmin(p));
        }
        guiManager.open(gui);
    }

    private void openPlayer(Player player) {
        GuiSession gui = page(player, "Spieler Commands");
        action(gui, 11, Material.BOOK, "/commands", "Oeffnet dieses Befehlsmenue.", "commands");
        action(gui, 13, Material.CHEST, "/kit", "Oeffnet deine verfuegbaren Rang-Kits.", "kit");
        action(gui, 15, Material.EMERALD, "/raenge", "Zeigt alle Raenge und deinen Fortschritt.", "raenge");
        back(gui);
        guiManager.open(gui);
    }

    private void openRanks(Player player) {
        GuiSession gui = page(player, "Raenge & Kits");
        action(gui, 11, Material.EXP_BOTTLE, "/rankup", "Kauft den naechsten Free-Rang mit Coins.", "rankup");
        action(gui, 13, Material.CHEST, "/kit", "Oeffnet die Kit-Auswahl.", "kit");
        action(gui, 15, Material.DIAMOND, "/raenge", "Oeffnet die komplette Rang-Uebersicht.", "raenge");
        back(gui);
        guiManager.open(gui);
    }

    private void openCrates(Player player) {
        GuiSession gui = page(player, "Crates");
        gui.setItem(11, icon(Material.SKULL_ITEM, ChatColor.GOLD + "Crate-Item",
                ChatColor.GRAY + "Linksklick: Rewards ansehen",
                ChatColor.GRAY + "Rechtsklick: eine Crate oeffnen",
                ChatColor.GRAY + "Shift + Rechtsklick: Open-All ab Exile"));
        action(gui, 15, Material.NETHER_STAR, "/craterewards",
                "Paid-Rank-Rewards mit eigenen Cooldowns.", "craterewards");
        back(gui);
        guiManager.open(gui);
    }

    private void openPerks(Player player) {
        GuiSession gui = page(player, "Perks");
        action(gui, 10, Material.FEATHER, "/fly", "Flugmodus fuer Knight+ oder Fly-Recht.", "fly");
        action(gui, 12, Material.CHEST, "/stack", "Stackt gleiche Inventar-Items.", "stack");
        action(gui, 14, Material.BRICK, "/bloecke", "Oeffnet die No-Sell-Buildbloecke.", "bloecke");
        action(gui, 16, Material.ANVIL, "/repair", "Repariert das Item in deiner Hand.", "repair");
        back(gui);
        guiManager.open(gui);
    }

    private void openAdmin(Player player) {
        GuiSession gui = page(player, "Admin Commands");
        gui.setItem(10, infoCommand(Material.NAME_TAG, "/rang <Spieler> <Rang>",
                "Setzt den SkyKings-Rang eines Online-Spielers."));
        gui.setItem(12, infoCommand(Material.PAPER, "/rechte <Spieler> <Recht>",
                "Vergibt ein freigegebenes Gutschein-/Perk-Recht."));
        gui.setItem(14, infoCommand(Material.CHEST, "/crate give <Spieler> <Typ> [Anzahl]",
                "Gibt Crates mit eindeutiger Seriennummer aus."));
        action(gui, 16, Material.EMERALD, "/gutscheine",
                "Oeffnet die sichere Gutschein-Erzeugung.", "gutscheine");
        back(gui);
        guiManager.open(gui);
    }

    private void action(GuiSession gui, int slot, Material material, String shownCommand,
                        String description, String command) {
        gui.setItem(slot, command(material, shownCommand, description),
                (p, e, s) -> run(p, command));
    }

    private GuiSession page(Player player, String title) {
        return GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | " + title, 27);
    }

    private void back(GuiSession gui) {
        gui.setItem(22, icon(Material.ARROW, ChatColor.YELLOW + "Zurueck", "Zur Hauptuebersicht"),
                (p, e, s) -> open(p));
    }

    private void run(Player player, String command) {
        player.closeInventory();
        player.performCommand(command);
    }

    private ItemStack command(Material material, String command, String description) {
        return icon(material, ChatColor.WHITE + command,
                ChatColor.GRAY + description, "", ChatColor.YELLOW + "Klicken zum Ausfuehren");
    }

    private ItemStack infoCommand(Material material, String command, String description) {
        return icon(material, ChatColor.WHITE + command,
                ChatColor.GRAY + description, "", ChatColor.DARK_GRAY + "Befehl mit Argumenten");
    }

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
