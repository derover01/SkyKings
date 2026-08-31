package net.skykings.combat.map;

import net.skykings.combat.map.route.MapRouteService;
import net.skykings.combat.map.secret.SecretDiscoveryService;
import net.skykings.combat.map.zone.EndZoneService;
import net.skykings.combat.map.zone.HotZoneService;
import net.skykings.combat.map.zone.KingAltarService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** 6x9 Staff-Uebersicht fuer den aktuellen Phase-6-Map-Setup-Stand. */
public final class MapSetupCommand implements CommandExecutor {
    private final KingAltarService king;
    private final HotZoneService hotZones;
    private final EndZoneService endZone;
    private final SecretDiscoveryService secrets;
    private final MapLandmarkService landmarks;
    private final MapRouteService routes;
    private final TrashBinService trashBins;
    private final MapDisplayService displays;

    public MapSetupCommand(KingAltarService king, HotZoneService hotZones, EndZoneService endZone,
                           SecretDiscoveryService secrets, MapLandmarkService landmarks,
                           MapRouteService routes, TrashBinService trashBins, MapDisplayService displays) {
        this.king = king;
        this.hotZones = hotZones;
        this.endZone = endZone;
        this.secrets = secrets;
        this.landmarks = landmarks;
        this.routes = routes;
        this.trashBins = trashBins;
        this.displays = displays;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "SkyKings | Map Setup");
        inv.setItem(10, status(Material.GOLD_BLOCK, "King Altar", king.getZone() != null,
                king.getZone() == null ? "/kingaltar set <Radius>" : "Eingerichtet"));
        inv.setItem(12, info(Material.REDSTONE_BLOCK, "Hot Zones", hotZones.getZones().size(), "/hotzone add <Name> <Radius>"));
        inv.setItem(14, status(Material.ENDER_STONE, "End Zone", endZone.getZone() != null,
                endZone.getZone() == null ? "/endzone set <Radius>" : "Eingerichtet"));
        inv.setItem(16, info(Material.ENDER_PEARL, "Secrets", secrets.getSecrets().size(), "/secret add <Name> <Radius>"));
        inv.setItem(28, info(Material.COMPASS, "Landmarks", landmarks.list().size(), "/landmark set <Typ> <Radius>"));
        inv.setItem(30, info(Material.FEATHER, "Jump/Pearl Routes", routes.count(), "/route create <Name>"));
        inv.setItem(32, info(Material.HOPPER, "Trash Bins", trashBins.count(), "/trashbin add"));
        inv.setItem(34, info(Material.NAME_TAG, "Map Displays", displays.list().size(), "/mapdisplay set <Typ>"));
        inv.setItem(49, item(Material.BOOK, ChatColor.GOLD + "Setup-Reihenfolge",
                ChatColor.GRAY + "1. Spawn + Zonen",
                ChatColor.GRAY + "2. Insel-Landmarks + NPCs",
                ChatColor.GRAY + "3. Loot + Supply Points",
                ChatColor.GRAY + "4. Secrets + Routes",
                ChatColor.GRAY + "5. Displays + Trash Bins"));
        player.openInventory(inv);
        return true;
    }

    private ItemStack status(Material material, String name, boolean ready, String hint) {
        return item(material, (ready ? ChatColor.GREEN : ChatColor.RED) + name,
                (ready ? ChatColor.GREEN + "STATUS: OK" : ChatColor.RED + "STATUS: FEHLT"), ChatColor.GRAY + hint);
    }

    private ItemStack info(Material material, String name, int count, String hint) {
        return item(material, ChatColor.GOLD + name, ChatColor.GRAY + "Anzahl: " + ChatColor.WHITE + count, ChatColor.GRAY + hint);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        List<String> lines = new ArrayList<String>();
        for (String line : lore) lines.add(line);
        meta.setLore(lines);
        stack.setItemMeta(meta);
        return stack;
    }
}
