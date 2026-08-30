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

    public CommandsGui(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Commands", 27);
        gui.setItem(10, icon(Material.COMPASS, ChatColor.AQUA + "Spieler",
                "Grundlegende Server-Befehle"), (p, e, s) -> openPlayer(p));
        gui.setItem(12, icon(Material.DIAMOND_CHESTPLATE, ChatColor.GOLD + "Raenge & Kits",
                "Rangaufstieg, Kits und Uebersicht"), (p, e, s) -> openRanks(p));
        gui.setItem(14, icon(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Crates",
                "Crates, Rewards und Oeffnen"), (p, e, s) -> openCrates(p));
        gui.setItem(16, icon(Material.FEATHER, ChatColor.GREEN + "Perks",
                "Rang- und Gutschein-Perks"), (p, e, s) -> openPerks(p));
        if (player.isOp() || player.hasPermission("skykings.admin.commands")) {
            gui.setItem(22, icon(Material.REDSTONE_TORCH_ON, ChatColor.RED + "Administration",
                    "Rang-, Rechte- und Crate-Verwaltung"), (p, e, s) -> openAdmin(p));
        }
        guiManager.open(gui);
    }

    private void openPlayer(Player player) {
        GuiSession gui = page(player, "Spieler Commands");
        gui.setItem(11, command(Material.BOOK, "/commands", "Oeffnet dieses Befehlsmenue."),
                (p, e, s) -> run(p, "commands"));
        gui.setItem(13, command(Material.CHEST, "/kit", "Oeffnet deine verfuegbaren Rang-Kits."),
                (p, e, s) -> run(p, "kit"));
        gui.setItem(15, command(Material.EMERALD, "/raenge", "Zeigt alle Raenge und deinen Fortschritt."),
                (p, e, s) -> run(p, "raenge"));
        back(gui, player);
        guiManager.open(gui);
    }

    private void openRanks(Player player) {
        GuiSession gui = page(player, "Raenge & Kits");
        gui.setItem(11, command(Material.EXP_BOTTLE, "/rankup", "Kauft den naechsten Free-Rang mit Coins."),
                (p, e, s) -> run(p, "rankup"));
        gui.setItem(13, command(Material.CHEST, "/kit", "Oeffnet die Kit-Auswahl."),
                (p, e, s) -> run(p, "kit"));
        gui.setItem(15, command(Material.DIAMOND, "/raenge", "Oeffnet die komplette Rang-Uebersicht."),
                (p, e, s) -> run(p, "raenge"));
        back(gui, player);
        guiManager.open(gui);
    }

    private void openCrates(Player player) {
        GuiSession gui = page(player, "Crates");
        gui.setItem(11, icon(Material.SKULL_ITEM, ChatColor.GOLD + "Crate-Item",
                ChatColor.GRAY + "Linksklick: Rewards ansehen",
                ChatColor.GRAY + "Rechtsklick: eine Crate oeffnen",
                ChatColor.GRAY + "Shift + Rechtsklick: Open-All ab Exile"));
        gui.setItem(15, command(Material.NETHER_STAR, "/craterewards",
                "Rank-Rewards und deren Cooldowns.", "Wird in Phase 4 freigeschaltet."));
        back(gui, player);
        guiManager.open(gui);
    }

    private void openPerks(Player player) {
        GuiSession gui = page(player, "Perks");
        gui.setItem(10, command(Material.FEATHER, "/fly", "Flugmodus fuer Knight+ oder Fly-Recht."),
                (p, e, s) -> run(p, "fly"));
        gui.setItem(12, command(Material.CHEST, "/stack", "Stackt gleiche Inventar-Items."),
                (p, e, s) -> run(p, "stack"));
        gui.setItem(14, command(Material.BRICK, "/bloecke", "Oeffnet die No-Sell-Buildbloecke."),
                (p, e, s) -> run(p, "bloecke"));
        gui.setItem(16, command(Material.ANVIL, "/repair", "Repariert das Item in deiner Hand."),
                (p, e, s) -> run(p, "repair"));
        back(gui, player);
        guiManager.open(gui);
    }

    private void openAdmin(Player player) {
        GuiSession gui = page(player, "Admin Commands");
        gui.setItem(10, command(Material.NAME_TAG, "/rang <Spieler> <Rang>",
                "Setzt den SkyKings-Rang eines Online-Spielers."));
        gui.setItem(12, command(Material.PAPER, "/rechte <Spieler> <Recht>",
                "Vergibt ein freigegebenes Gutschein-/Perk-Recht."));
        gui.setItem(14, command(Material.CHEST, "/crate give <Spieler> <Typ> [Anzahl]",
                "Gibt echte Crate-Items mit eindeutiger Seriennummer aus."));
        gui.setItem(16, icon(Material.REDSTONE, ChatColor.RED + "Admin-Hinweis",
                ChatColor.GRAY + "Argument-Befehle werden hier nur angezeigt,",
                ChatColor.GRAY + "nicht automatisch ausgefuehrt."));
        back(gui, player);
        guiManager.open(gui);
    }

    private GuiSession page(Player player, String title) {
        return GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | " + title, 27);
    }

    private void back(GuiSession gui, Player player) {
        gui.setItem(22, icon(Material.ARROW, ChatColor.YELLOW + "Zurueck", "Zur Hauptuebersicht"),
                (p, e, s) -> open(p));
    }

    private void run(Player player, String command) {
        player.closeInventory();
        player.performCommand(command);
    }

    private ItemStack command(Material material, String command, String... description) {
        List<String> lore = new ArrayList<String>();
        for (String line : description) lore.add(ChatColor.GRAY + line);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Klicken zum Ausfuehren");
        return icon(material, ChatColor.WHITE + command, lore.toArray(new String[0]));
    }

    private ItemStack icon(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(loreLines));
        item.setItemMeta(meta);
        return item;
    }
}
