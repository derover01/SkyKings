package net.skykings.combat.map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Persistente Map-Muelleimer: Rechtsklick auf markierten Hopper oeffnet ein 6x9 Wegwerf-Inventar. */
public final class TrashBinService implements Listener, CommandExecutor {
    private final JavaPlugin plugin;
    private final File file;
    private final Set<String> bins = new LinkedHashSet<String>();

    public TrashBinService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "trash-bins.yml");
        load();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (!bins.contains(key(block.getLocation()))) return;
        event.setCancelled(true);
        Inventory inventory = plugin.getServer().createInventory(null, 54, ChatColor.DARK_GRAY + "SkyKings | Trash");
        event.getPlayer().openInventory(inventory);
        event.getPlayer().sendMessage(ChatColor.GRAY + "Alles, was du hier hineinlegst, wird beim Schliessen geloescht.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!bins.contains(key(event.getBlock().getLocation()))) return;
        if (event.getPlayer().hasPermission("skykings.admin.map")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Dieser Muelleimer gehoert zur Map.");
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
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/trashbin add" + ChatColor.GRAY + " - Hopper ansehen");
            player.sendMessage(ChatColor.YELLOW + "/trashbin remove" + ChatColor.GRAY + " - markierten Hopper ansehen");
            player.sendMessage(ChatColor.YELLOW + "/trashbin list");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("list".equals(sub)) {
            player.sendMessage(ChatColor.GOLD + "Trash-Bins: " + ChatColor.WHITE + bins.size());
            return true;
        }
        Block target = player.getTargetBlock((Set<Material>) null, 6);
        if (target == null || target.getType() != Material.HOPPER) {
            player.sendMessage(ChatColor.RED + "Du musst einen Hopper innerhalb von 6 Bloecken ansehen.");
            return true;
        }
        String key = key(target.getLocation());
        if ("add".equals(sub) || "set".equals(sub)) {
            bins.add(key);
            save();
            player.sendMessage(ChatColor.GREEN + "Trash-Bin registriert.");
            return true;
        }
        if ("remove".equals(sub) || "delete".equals(sub)) {
            if (bins.remove(key)) {
                save();
                player.sendMessage(ChatColor.YELLOW + "Trash-Bin entfernt.");
            } else player.sendMessage(ChatColor.RED + "Dieser Hopper ist keine Trash-Bin.");
            return true;
        }
        return true;
    }

    private void load() {
        bins.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        bins.addAll(yaml.getStringList("bins"));
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("bins", new java.util.ArrayList<String>(bins));
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("trash-bins.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private String key(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
