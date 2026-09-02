package net.skykings.core.command;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.ui.UiItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Premium 6x9 Help Hub: wenige klare Kategorien statt einer Command-Item-Wand. */
public final class CommandsGui {

    private final GuiManager guiManager;

    public CommandsGui(GuiManager guiManager) { this.guiManager = guiManager; }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Hilfe", 54);

        gui.setItem(4, UiItems.head(player.getName(),
                ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS HILFE",
                ChatColor.GRAY + "Commands nach System sortiert.",
                ChatColor.DARK_GRAY + "Klick eine Kategorie."));

        category(gui, 10, Material.DIAMOND_SWORD, ChatColor.RED + "PvP & Profil",
                "Stats, Bounties, Cosmetics", p -> openPvp(p));
        category(gui, 12, Material.GOLD_INGOT, ChatColor.GOLD + "Economy & Handel",
                "Shop, Trade, Casino, Jackpot", p -> openEconomy(p));
        category(gui, 14, Material.GRASS, ChatColor.GREEN + "Islands & Plots",
                "Private Welten & Schutz", p -> openClaims(p));
        category(gui, 16, Material.EXP_BOTTLE, ChatColor.AQUA + "Progression",
                "Ranks, Kits, Season, Pass", p -> openProgression(p));
        category(gui, 28, Material.BLAZE_ROD, ChatColor.LIGHT_PURPLE + "Events",
                "Duel, LMS, Clan War, Most Wanted", p -> openEvents(p));
        category(gui, 30, Material.NAME_TAG, ChatColor.YELLOW + "Social",
                "Clan, Peace, Collection", p -> openSocial(p));
        category(gui, 32, Material.FEATHER, ChatColor.WHITE + "Komfort & Perks",
                "Fly, EC, Repair, Tools", p -> openPerks(p));
        category(gui, 34, Material.COMPASS, ChatColor.AQUA + "Travel & Warps",
                "Spawn und Schnellreisen", p -> openTravel(p));
        category(gui, 40, Material.NETHER_STAR, ChatColor.GOLD + "Crates & Rewards",
                "Crates, Rewards, Daily", p -> openCrates(p));

        if (hasTeamAccess(player)) {
            gui.setItem(47, UiItems.item(Material.BOOK_AND_QUILL,
                    ChatColor.BLUE.toString() + ChatColor.BOLD + "TEAM",
                    ChatColor.GRAY + "Moderation & Servertools",
                    UiItems.action("Klicken")), (p,e,s) -> openTeam(p));
        }
        if (player.hasPermission("skykings.admin.commands")) {
            gui.setItem(51, UiItems.item(Material.REDSTONE_TORCH_ON,
                    ChatColor.RED.toString() + ChatColor.BOLD + "ADMIN",
                    ChatColor.GRAY + "Setup, Diagnose, Verwaltung",
                    UiItems.action("Klicken")), (p,e,s) -> openAdmin(p));
        }

        gui.setItem(49, UiItems.item(Material.BOOK,
                ChatColor.AQUA + "Quick Help",
                ChatColor.GRAY + "/commands jederzeit öffnen",
                ChatColor.DARK_GRAY + "Aliases: /befehle, /cmds"));
        guiManager.open(gui);
    }

    private void openPvp(Player player) {
        GuiSession gui = page(player, "PvP & Profil");
        action(gui, 10, Material.DIAMOND_SWORD, "/stats", "Profil & PvP-Stats", "stats");
        action(gui, 12, Material.GOLD_INGOT, "/top", "Leaderboards & Bounties", "top");
        action(gui, 14, Material.BLAZE_POWDER, "/killeffect", "Cosmetics Center", "killeffect");
        action(gui, 16, Material.SKULL_ITEM, "/collection", "Head Collection", "collection");
        action(gui, 28, Material.IRON_SWORD, "/stattrack", "Weapon History", "stattrack");
        action(gui, 30, Material.MAP, "/mapmastery", "Map-Fortschritt", "mapmastery");
        action(gui, 32, Material.DIAMOND, "/achievements", "Achievements", "achievements");
        action(gui, 34, Material.NETHER_STAR, "/medals", "Season-Medaillen", "medals");
        nav(gui);
        guiManager.open(gui);
    }

    private void openEconomy(Player player) {
        GuiSession gui = page(player, "Economy & Handel");
        action(gui, 10, Material.EMERALD, "/shop", "Systemshop öffnen", "shop");
        action(gui, 12, Material.PAPER, "/worth", "Hand-Item bewerten", "worth");
        info(gui, 14, Material.GOLD_INGOT, "/sell <hand|all>", "Items verkaufen");
        info(gui, 16, Material.CHEST, "/trade <Spieler>", "Sicherer Spielerhandel");
        action(gui, 22, Material.REDSTONE, "/casino", "Void Crown Casino", "casino");
        action(gui, 28, Material.NETHER_STAR, "/jackpot", "Serverweiter Coin-Jackpot", "jackpot");
        info(gui, 30, Material.MONSTER_EGG, "/playershop kaufen", "Eigenen Händler starten");
        action(gui, 32, Material.EMERALD_BLOCK, "/shoprent", "Geschützten Marktstand mieten", "shoprent");
        info(gui, 34, Material.HOPPER, "Shift + Rechtsklick", "Angebote, Erlös & Shop entfernen");
        nav(gui);
        guiManager.open(gui);
    }

    private void openClaims(Player player) {
        GuiSession gui = page(player, "Islands & Plots");
        action(gui, 10, Material.GRASS, "/is", "Island Hub", "is");
        info(gui, 12, Material.SAPLING, "/is create", "Neue Insel erstellen");
        info(gui, 14, Material.COMPASS, "/is level / top", "Level & Topliste");
        action(gui, 16, Material.STONE, "/plot", "Plot Hub", "plot");
        info(gui, 28, Material.WOOD_STEP, "/p auto / rand", "Claim & Rand");
        info(gui, 30, Material.BRICK, "/p merge <Richtung>", "Plots verbinden");
        info(gui, 32, Material.NAME_TAG, "/p add / trust", "Baurechte vergeben");
        action(gui, 34, Material.MOB_SPAWNER, "/spawnerstack", "Spawner-Stack prüfen", "spawnerstack");
        nav(gui);
        guiManager.open(gui);
    }

    private void openProgression(Player player) {
        GuiSession gui = page(player, "Progression");
        action(gui, 10, Material.DIAMOND_CHESTPLATE, "/kit", "Kit Arsenal", "kit");
        action(gui, 12, Material.EXP_BOTTLE, "/rankup", "Nächsten Free-Rang kaufen", "rankup");
        action(gui, 14, Material.DIAMOND, "/raenge", "Rang-Übersicht", "raenge");
        action(gui, 16, Material.NETHER_STAR, "/battlepass", "Battle Pass", "battlepass");
        action(gui, 28, Material.BOOK, "/quests", "Quest Center", "quests");
        action(gui, 30, Material.WATCH, "/season", "Season-XP", "season");
        action(gui, 32, Material.EXP_BOTTLE, "/pvplevel", "PvP-Level 1-100", "pvplevel");
        action(gui, 34, Material.GOLD_NUGGET, "/dailyrewards", "Daily Reward", "dailyrewards");
        nav(gui);
        guiManager.open(gui);
    }

    private void openEvents(Player player) {
        GuiSession gui = page(player, "Events");
        info(gui, 10, Material.DIAMOND_SWORD, "/duel <Spieler>", "Setup: Kit + Coin-Einsatz");
        info(gui, 12, Material.FIREWORK, "/lms join", "Last Man Standing");
        info(gui, 14, Material.BANNER, "/clanwar <Clan-Owner>", "2v2 bis 5v5 Clan War");
        info(gui, 16, Material.COMPASS, "/targetevent status", "Most Wanted");
        info(gui, 30, Material.GOLD_INGOT, "/verlosung join", "Coin-Verlosung");
        nav(gui);
        guiManager.open(gui);
    }

    private void openSocial(Player player) {
        GuiSession gui = page(player, "Social");
        action(gui, 11, Material.NAME_TAG, "/clan", "Clan verwalten", "clan");
        info(gui, 13, Material.RED_ROSE, "/peace <Spieler>", "Peace anfragen");
        action(gui, 15, Material.SKULL_ITEM, "/collection", "Head Collection", "collection");
        action(gui, 29, Material.BOOK, "/stats", "Eigenes Profil", "stats");
        action(gui, 33, Material.NETHER_STAR, "/legacyhall", "Hall of Fame", "legacyhall");
        nav(gui);
        guiManager.open(gui);
    }

    private void openPerks(Player player) {
        GuiSession gui = page(player, "Komfort & Perks");
        action(gui, 10, Material.FEATHER, "/fly", "Flugmodus", "fly");
        info(gui, 12, Material.SUGAR, "/speed <1-10|reset>", "Fluggeschwindigkeit");
        action(gui, 14, Material.CHEST, "/stack", "Items stapeln", "stack");
        action(gui, 16, Material.BRICK, "/bloecke", "Baublock-Katalog", "bloecke");
        action(gui, 28, Material.ANVIL, "/repair", "Hand-Item reparieren", "repair");
        action(gui, 30, Material.ENDER_CHEST, "/ec", "Enderchest", "ec");
        action(gui, 32, Material.WORKBENCH, "/workbench", "Werkbank", "workbench");
        action(gui, 34, Material.LAVA_BUCKET, "/trash", "Temporärer Mülleimer", "trash");
        nav(gui);
        guiManager.open(gui);
    }

    private void openTravel(Player player) {
        GuiSession gui = page(player, "Travel & Warps");
        action(gui, 12, Material.COMPASS, "/warp", "Warp-Auswahl", "warp");
        info(gui, 14, Material.ENDER_PEARL, "/warp <Name>", "3s Direktreise");
        action(gui, 16, Material.BED, "/spawn", "3s zum Spawn", "spawn");
        gui.setItem(31, UiItems.item(Material.WATCH,
                ChatColor.YELLOW + "Sicherheitsregel",
                ChatColor.GRAY + "Combat blockiert Travel.",
                ChatColor.GRAY + "Bewegung/Schaden bricht ab."));
        nav(gui);
        guiManager.open(gui);
    }

    private void openCrates(Player player) {
        GuiSession gui = page(player, "Crates & Rewards");
        gui.setItem(11, UiItems.item(Material.SKULL_ITEM,
                ChatColor.GOLD + "Crate Item",
                ChatColor.GRAY + "Links: Rewards ansehen",
                ChatColor.GRAY + "Rechts: öffnen"));
        action(gui, 13, Material.NETHER_STAR, "/craterewards", "Rang-Rewards", "craterewards");
        action(gui, 15, Material.GOLD_NUGGET, "/dailyrewards", "Daily Reward", "dailyrewards");
        action(gui, 29, Material.GOLD_INGOT, "/jackpot", "Coin Jackpot", "jackpot");
        action(gui, 31, Material.NETHER_STAR, "/battlepass", "Season Rewards", "battlepass");
        nav(gui);
        guiManager.open(gui);
    }

    private void openTeam(Player player) {
        GuiSession gui = page(player, "Team");
        if (player.hasPermission("skykings.staff.announcement")) info(gui, 10, Material.PAPER, "/announcement <Text>", "Server-Ankündigung");
        if (player.hasPermission("skykings.staff.clearchat")) action(gui, 12, Material.EMPTY_MAP, "/clearchat", "Serverchat leeren", "clearchat");
        if (player.hasPermission("skykings.staff.clear")) action(gui, 14, Material.HOPPER, "/clear", "Boden-Clear starten", "clear");
        if (player.hasPermission("skykings.staff.gamemode")) info(gui, 16, Material.GRASS, "/gm <0|1|2|3>", "Gamemode ändern");
        if (player.hasPermission("skykings.admin.buildmode")) action(gui, 30, Material.GOLD_PICKAXE, "/buildmode", "Map-Baumodus", "buildmode");
        nav(gui);
        guiManager.open(gui);
    }

    private void openAdmin(Player player) {
        GuiSession gui = page(player, "Administration");
        info(gui, 10, Material.NAME_TAG, "/rang <Spieler> <Rang>", "Rang setzen");
        info(gui, 12, Material.PAPER, "/rechte <Spieler> <Recht>", "Recht vergeben");
        info(gui, 14, Material.CHEST, "/crate give ...", "Crates ausgeben");
        action(gui, 16, Material.EMERALD, "/gutscheine", "Gutscheine erstellen", "gutscheine");
        if (player.hasPermission("skykings.admin.coins")) {
            info(gui, 20, Material.GOLD_INGOT, "/addcoins <Spieler> <Anzahl>", "Coins sicher hinzufügen");
            info(gui, 22, Material.GOLD_BLOCK, "/setcoins <Spieler> <Anzahl>", "Kontostand setzen");
        }
        if (player.hasPermission("skykings.admin.island.starterreset")) {
            info(gui, 24, Material.SAPLING, "/is resetstarter <Spieler>", "Island-Starterclaim recovern");
        }
        action(gui, 28, Material.REDSTONE_TORCH_ON, "/mapsetup", "Map Setup Hub", "mapsetup");
        info(gui, 30, Material.COMPASS, "/maptp <Ziel>", "Map-Schnellreise");
        action(gui, 32, Material.REDSTONE_BLOCK, "/skcheck", "Runtime prüfen", "skcheck");
        info(gui, 34, Material.SIGN, "/setwarp <Name>", "Warp setzen");
        if (player.hasPermission("skykings.admin.shoprents"))
            info(gui, 38, Material.EMERALD_BLOCK, "/shoprent pos1 / pos2", "Marktstände definieren");
        gui.setItem(40, UiItems.item(Material.BOOK,
                ChatColor.RED + "Admin Hinweis",
                ChatColor.GRAY + "Finale Positionen ingame setzen.",
                ChatColor.DARK_GRAY + "Keine Koordinaten raten."));
        nav(gui);
        guiManager.open(gui);
    }

    private void category(GuiSession gui, int slot, Material material, String title, String description, OpenAction action) {
        gui.setItem(slot, UiItems.item(material, title,
                ChatColor.GRAY + description,
                UiItems.action("Kategorie öffnen")), (p,e,s) -> action.open(p));
    }

    private void action(GuiSession gui, int slot, Material material, String shownCommand, String description, String command) {
        gui.setItem(slot, command(material, shownCommand, description), (p,e,s) -> run(p, command));
    }

    private void info(GuiSession gui, int slot, Material material, String shownCommand, String description) {
        gui.setItem(slot, UiItems.item(material,
                ChatColor.WHITE + shownCommand,
                ChatColor.GRAY + description,
                ChatColor.DARK_GRAY + "Mit Argumenten im Chat nutzen"));
    }

    private GuiSession page(Player player, String title) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | " + title, 54);
        gui.setItem(4, UiItems.item(Material.BOOK,
                ChatColor.AQUA.toString() + ChatColor.BOLD + title.toUpperCase(),
                ChatColor.GRAY + "Kurze Command-Cards statt Textwand."));
        return gui;
    }

    private void nav(GuiSession gui) {
        gui.setItem(45, UiItems.back(), (p,e,s) -> open(p));
        gui.setItem(49, UiItems.home(), (p,e,s) -> open(p));
    }

    private boolean hasTeamAccess(Player player) {
        return player.hasPermission("skykings.staff.announcement")
                || player.hasPermission("skykings.staff.clearchat")
                || player.hasPermission("skykings.staff.clear")
                || player.hasPermission("skykings.staff.gamemode")
                || player.hasPermission("skykings.admin.buildmode");
    }

    private void run(Player player, String command) {
        player.closeInventory();
        player.performCommand(command);
    }

    private ItemStack command(Material material, String command, String description) {
        return UiItems.item(material,
                ChatColor.WHITE + command,
                ChatColor.GRAY + description,
                UiItems.action("Klicken zum Ausführen"));
    }

    private interface OpenAction { void open(Player player); }
}
