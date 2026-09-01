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

/** Zentrales SkyKings-Befehlsmenü mit kurzen, scanbaren Beschreibungen. */
public final class CommandsGui {

    private final GuiManager guiManager;

    public CommandsGui(GuiManager guiManager) { this.guiManager = guiManager; }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Commands", 27);
        gui.setItem(10, icon(Material.COMPASS, ChatColor.AQUA + "Spieler", "Wichtige Server-Befehle"), (p,e,s) -> openPlayer(p));
        gui.setItem(12, icon(Material.DIAMOND_CHESTPLATE, ChatColor.GOLD + "Ränge & Kits", "Ränge, Fortschritt & Kits"), (p,e,s) -> openRanks(p));
        gui.setItem(14, icon(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Crates", "Crates & Rewards"), (p,e,s) -> openCrates(p));
        gui.setItem(16, icon(Material.FEATHER, ChatColor.GREEN + "Perks", "Deine freigeschalteten Perks"), (p,e,s) -> openPerks(p));
        if (player.hasPermission("skykings.staff.announcement") || player.hasPermission("skykings.staff.clearchat")) {
            gui.setItem(20, icon(Material.BOOK_AND_QUILL, ChatColor.BLUE + "Team", "Moderation & Ankündigungen"), (p,e,s) -> openTeam(p));
        }
        if (player.hasPermission("skykings.admin.commands")) {
            gui.setItem(24, icon(Material.REDSTONE_TORCH_ON, ChatColor.RED + "Administration", "Admin-Werkzeuge"), (p,e,s) -> openAdmin(p));
        }
        guiManager.open(gui);
    }

    private void openPlayer(Player player) {
        GuiSession gui = page(player, "Spieler Commands");
        action(gui, 9, Material.BOOK, "/commands", "Befehlsübersicht öffnen.", "commands");
        action(gui, 10, Material.GOLD_INGOT, "/top", "PvP-Leaderboards ansehen.", "top");
        action(gui, 11, Material.DIAMOND_SWORD, "/stats", "Deine PvP-Statistiken.", "stats");
        action(gui, 12, Material.BLAZE_POWDER, "/killeffect", "Kill-Effekt auswählen.", "killeffect");
        action(gui, 13, Material.CHEST, "/kit", "Deine Rang-Kits öffnen.", "kit");
        action(gui, 15, Material.EMERALD, "/ränge", "Ränge & Fortschritt.", "raenge");
        action(gui, 17, Material.LAVA_BUCKET, "/trash / /müll", "Temporären Mülleimer öffnen.", "trash");
        back(gui);
        guiManager.open(gui);
    }

    private void openRanks(Player player) {
        GuiSession gui = page(player, "Ränge & Kits");
        action(gui, 11, Material.EXP_BOTTLE, "/rankup", "Nächsten Free-Rang kaufen.", "rankup");
        action(gui, 13, Material.CHEST, "/kit", "Kit-Auswahl öffnen.", "kit");
        action(gui, 15, Material.DIAMOND, "/ränge", "Rang-Übersicht öffnen.", "raenge");
        back(gui);
        guiManager.open(gui);
    }

    private void openCrates(Player player) {
        GuiSession gui = page(player, "Crates");
        gui.setItem(11, icon(Material.SKULL_ITEM, ChatColor.GOLD + "Crate-Item",
                ChatColor.GRAY + "Links: Rewards",
                ChatColor.GRAY + "Rechts: Öffnen"));
        action(gui, 15, Material.NETHER_STAR, "/craterewards", "Rang-Rewards abholen.", "craterewards");
        back(gui);
        guiManager.open(gui);
    }

    private void openPerks(Player player) {
        GuiSession gui = page(player, "Perks");
        action(gui, 9, Material.FEATHER, "/fly", "Flugmodus umschalten.", "fly");
        action(gui, 11, Material.CHEST, "/stack", "Gleiche Items stapeln.", "stack");
        action(gui, 13, Material.BRICK, "/blöcke", "Baublock-Katalog öffnen.", "bloecke");
        action(gui, 15, Material.ANVIL, "/repair", "Hand-Item reparieren.", "repair");
        action(gui, 17, Material.ENDER_CHEST, "/ec", "Enderchest öffnen.", "ec");
        back(gui);
        guiManager.open(gui);
    }

    private void openTeam(Player player) {
        GuiSession gui = page(player, "Team Commands");
        if (player.hasPermission("skykings.staff.announcement")) gui.setItem(11, infoCommand(Material.PAPER, "/announcement <Text>", "Server-Ankündigung senden."));
        if (player.hasPermission("skykings.staff.clearchat")) action(gui, 13, Material.EMPTY_MAP, "/clearchat / /cc", "Serverchat leeren.", "clearchat");
        if (player.hasPermission("skykings.staff.gamemode")) gui.setItem(15, infoCommand(Material.GRASS, "/gm <0|1|2|3> [Spieler]", "Gamemode ändern."));
        back(gui);
        guiManager.open(gui);
    }

    private void openAdmin(Player player) {
        GuiSession gui = page(player, "Admin Commands");
        gui.setItem(9, infoCommand(Material.NAME_TAG, "/rang <Spieler> <Rang>", "Spielerrang setzen."));
        gui.setItem(10, infoCommand(Material.PAPER, "/rechte <Spieler> <Recht>", "Feature-Recht vergeben."));
        gui.setItem(11, infoCommand(Material.CHEST, "/crate give <Spieler> <Typ> [Anzahl]", "Crates ausgeben."));
        action(gui, 12, Material.EMERALD, "/gutscheine", "Gutscheine erstellen.", "gutscheine");
        gui.setItem(14, infoCommand(Material.GRASS, "/gm <0|1|2|3> [Spieler]", "Gamemode ändern."));
        gui.setItem(16, icon(Material.SIGN, ChatColor.GREEN + "Free Sign",
                ChatColor.GRAY + "1: [FREE]",
                ChatColor.GRAY + "2: Item-ID",
                ChatColor.GRAY + "3: Menge"));
        back(gui);
        guiManager.open(gui);
    }

    private void action(GuiSession gui, int slot, Material material, String shownCommand, String description, String command) {
        gui.setItem(slot, command(material, shownCommand, description), (p,e,s) -> run(p, command));
    }

    private GuiSession page(Player player, String title) { return GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | " + title, 27); }
    private void back(GuiSession gui) { gui.setItem(22, icon(Material.ARROW, ChatColor.YELLOW + "Zurück", "Zur Übersicht"), (p,e,s) -> open(p)); }
    private void run(Player player, String command) { player.closeInventory(); player.performCommand(command); }
    private ItemStack command(Material material, String command, String description) { return icon(material, ChatColor.WHITE + command, ChatColor.GRAY + description, "", ChatColor.YELLOW + "Klicken zum Ausführen"); }
    private ItemStack infoCommand(Material material, String command, String description) { return icon(material, ChatColor.WHITE + command, ChatColor.GRAY + description, "", ChatColor.DARK_GRAY + "Mit Argumenten verwenden"); }
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
