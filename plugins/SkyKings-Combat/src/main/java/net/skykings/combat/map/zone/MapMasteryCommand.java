package net.skykings.combat.map.zone;

import net.skykings.combat.map.MapLandmarkService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Custom-Panel fuer persistente Map-/Island-Mastery. */
public final class MapMasteryCommand implements CommandExecutor {
    private final MapMasteryService mastery;
    private final GuiManager guiManager;

    public MapMasteryCommand(MapMasteryService mastery) {
        this(mastery, GuiManager.active());
    }

    public MapMasteryCommand(MapMasteryService mastery, GuiManager guiManager) {
        this.mastery = mastery;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }

        Player viewer = (Player) sender;
        Player target = viewer;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                viewer.sendMessage(UiTheme.DANGER + "Spieler nicht online.");
                SoundFeedback.error(viewer);
                return true;
            }
        }

        open(viewer, target);
        return true;
    }

    private void open(Player viewer, Player target) {
        UUID uuid = target.getUniqueId();
        GuiSession gui = GuiSession.create(viewer, UiTheme.title("Map Mastery"), 54);

        gui.setItem(4, UiItems.item(Material.NETHER_STAR,
                UiTheme.PRIMARY + "MAP MASTERY",
                UiTheme.TEXT + target.getName(),
                UiTheme.MUTED + "Titel: " + UiTheme.PRIMARY + mastery.getTitle(uuid),
                UiTheme.MUTED + "Erkunde, kaempfe und meistere die Map."), null);

        addLandmark(gui, uuid, 19, Material.GOLD_INGOT, MapLandmarkService.Type.GOLD, ChatColor.GOLD, "Gold Island");
        addLandmark(gui, uuid, 21, Material.EXP_BOTTLE, MapLandmarkService.Type.LEVEL, ChatColor.AQUA, "Level Island");
        addLandmark(gui, uuid, 23, Material.ANVIL, MapLandmarkService.Type.BLACKSMITH, ChatColor.GRAY, "Blacksmith Island");
        addLandmark(gui, uuid, 25, Material.EMERALD, MapLandmarkService.Type.MERCHANT, ChatColor.GREEN, "Merchant Island");

        gui.setItem(29, UiItems.item(Material.IRON_SWORD,
                UiTheme.DANGER + "Combat Mastery",
                UiTheme.MUTED + "Hot-Zone-Kills: " + UiTheme.TEXT + mastery.getHotZoneKills(uuid),
                UiTheme.MUTED + "End-Zone-Kills: " + UiTheme.TEXT + mastery.getEndKills(uuid),
                UiTheme.MUTED + "King-Captures: " + UiTheme.TEXT + mastery.getKingCaptures(uuid)), null);

        gui.setItem(31, UiItems.item(Material.WATCH,
                UiTheme.PRIMARY + "Exploration",
                UiTheme.MUTED + "Landmark-Zeit: " + UiTheme.TEXT + formatDuration(mastery.getTotalLandmarkSeconds(uuid)),
                UiTheme.MUTED + "Besuche: " + UiTheme.TEXT + mastery.getTotalLandmarkVisits(uuid),
                UiTheme.MUTED + "Aktivitaeten: " + UiTheme.TEXT + mastery.getTotalLandmarkActivities(uuid)), null);

        gui.setItem(33, UiItems.item(Material.BOOK,
                UiTheme.MYTHIC + "Discovery",
                UiTheme.MUTED + "Secrets gefunden: " + UiTheme.TEXT + mastery.getSecrets(uuid),
                UiTheme.MUTED + "Weitere Map-Aktivitaeten bauen",
                UiTheme.MUTED + "deinen Mastery-Titel aus."), null);

        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "stats");
        });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.COMPASS,
                UiTheme.PRIMARY + "Home",
                UiTheme.MUTED + "Zur SkyKings Uebersicht.",
                UiItems.action("Klicken")), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "commands");
        });

        guiManager.open(gui);
        SoundFeedback.menuOpen(viewer);
    }

    private void addLandmark(GuiSession gui, UUID uuid, int slot, Material material,
                             MapLandmarkService.Type type, ChatColor color, String name) {
        gui.setItem(slot, UiItems.item(material,
                color + name,
                UiTheme.MUTED + "Zeit: " + UiTheme.TEXT + formatDuration(mastery.getLandmarkSeconds(uuid, type)),
                UiTheme.MUTED + "Besuche: " + UiTheme.TEXT + mastery.getLandmarkVisits(uuid, type),
                UiTheme.MUTED + "Aktivitaeten: " + UiTheme.TEXT + mastery.getLandmarkActivities(uuid, type)), null);
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m";
        return seconds + "s";
    }
}
