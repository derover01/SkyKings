package net.skykings.admin.command;

import net.skykings.admin.warp.WarpService;
import net.skykings.admin.warp.WarpTeleportService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** /warp und /warps oeffnen das GUI, /warp <Name> startet direkt die sichere Schnellreise. */
public final class WarpCommand implements CommandExecutor, TabCompleter {
    private final WarpService warps;
    private final WarpTeleportService teleports;

    public WarpCommand(WarpService warps, WarpTeleportService teleports) {
        this.warps = warps;
        this.teleports = teleports;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            open(player);
            return true;
        }
        teleports.request(player, args[0]);
        return true;
    }

    private void open(Player player) {
        List<String> names = warps.names();
        if (names.isEmpty()) {
            player.sendMessage(UiTheme.WARNING + "Aktuell sind keine Warps eingerichtet.");
            SoundFeedback.error(player);
            return;
        }
        int size = names.size() <= 7 ? 27 : names.size() <= 16 ? 36 : names.size() <= 25 ? 45 : 54;
        GuiSession gui = GuiSession.create(player, UiTheme.title("Warps"), size);
        int slot = size == 27 ? 10 : 9;
        for (final String name : names) {
            while (slot % 9 == 0 || slot % 9 == 8) slot++;
            if (slot >= size - 9) break;
            gui.setItem(slot++, UiItems.item(iconFor(name),
                    UiTheme.PRIMARY + name,
                    UiTheme.MUTED + "3 Sekunden Schnellreise",
                    UiTheme.MUTED + "Nicht im Combat verfuegbar",
                    UiItems.action("Klicken zum Warpen")), (p,e,s) -> teleports.request(p, name));
        }
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private Material iconFor(String warpName) {
        String key = warpName == null ? "" : warpName.toLowerCase(Locale.ROOT)
                .replace(" ", "").replace("-", "").replace("_", "");
        if (key.contains("casino") || key.contains("voidcrown")) return Material.GOLD_INGOT;
        if (key.contains("crate") || key.contains("kiste")) return Material.CHEST;
        if (key.contains("shop") || key.contains("markt")) return Material.EMERALD;
        if (key.contains("plot")) return Material.GRASS;
        if (key.contains("island") || key.contains("insel")) return Material.FEATHER;
        if (key.contains("pvp") || key.contains("arena")) return Material.DIAMOND_SWORD;
        if (key.contains("event") || key.contains("community")) return Material.FIREWORK;
        if (key.contains("spawn") || key.contains("main") || key.contains("hub")) return Material.NETHER_STAR;
        if (key.contains("farm") || key.contains("mine")) return Material.DIAMOND_PICKAXE;
        if (key.contains("enchant")) return Material.ENCHANTMENT_TABLE;
        if (key.contains("blacksmith") || key.contains("schmied")) return Material.ANVIL;
        return Material.ENDER_PEARL;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String name : warps.names()) if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) result.add(name);
        return result;
    }
}
