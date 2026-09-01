package net.skykings.admin.casino;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Persistente physische Spielstationen fuer die Void-Crown-Casino-Map. */
public final class CasinoNpcService implements Listener, CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "skykings.admin.casinonpc";

    private enum Station { HUB, COINFLIP, DICE, LUCKY7, WHEEL, JACKPOT }

    private final JavaPlugin plugin;
    private final CasinoCommand casino;
    private final File file;
    private final Map<UUID, Station> bindings = new LinkedHashMap<UUID, Station>();

    public CasinoNpcService(JavaPlugin plugin, CasinoCommand casino) {
        this.plugin = plugin;
        this.casino = casino;
        this.file = new File(plugin.getDataFolder(), "casino-npcs.yml");
        load();
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        Station station = bindings.get(event.getRightClicked().getUniqueId());
        if (station == null) return;
        event.setCancelled(true);
        casino.openStation(event.getPlayer(), key(station));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(UiTheme.DANGER + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            usage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("bind".equals(sub)) {
            if (args.length < 2) {
                player.sendMessage(UiTheme.WARNING + "/casinonpc bind <hub|coinflip|dice|lucky7|wheel|jackpot>");
                return true;
            }
            Station station = parse(args[1]);
            if (station == null) {
                player.sendMessage(UiTheme.DANGER + "Unbekannte Casino-Station.");
                return true;
            }
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(UiTheme.DANGER + "Kein Villager innerhalb von 6 Bloecken gefunden.");
                return true;
            }
            bindings.put(villager.getUniqueId(), station);
            villager.setCustomName(displayName(station));
            villager.setCustomNameVisible(true);
            save();
            player.sendMessage(UiTheme.SUCCESS + "Casino-NPC gebunden: " + ChatColor.stripColor(displayName(station)));
            SoundFeedback.success(player);
            return true;
        }

        if ("unbind".equals(sub) || "remove".equals(sub)) {
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(UiTheme.DANGER + "Kein Villager innerhalb von 6 Bloecken gefunden.");
                return true;
            }
            Station removed = bindings.remove(villager.getUniqueId());
            if (removed == null) {
                player.sendMessage(UiTheme.DANGER + "Dieser Villager ist keine Casino-Station.");
                return true;
            }
            save();
            player.sendMessage(UiTheme.WARNING + "Casino-NPC-Bindung entfernt.");
            SoundFeedback.back(player);
            return true;
        }

        if ("info".equals(sub)) {
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(UiTheme.DANGER + "Kein Villager innerhalb von 6 Bloecken gefunden.");
                return true;
            }
            Station station = bindings.get(villager.getUniqueId());
            player.sendMessage(UiTheme.TEXT + "Void Crown Casino NPC");
            player.sendMessage(UiTheme.MUTED + "Station: " + (station == null ? UiTheme.DANGER + "nicht gebunden" : UiTheme.SUCCESS + station.name()));
            return true;
        }

        if ("list".equals(sub)) {
            player.sendMessage(UiTheme.TEXT + "Void Crown Casino NPCs: " + UiTheme.SUCCESS + bindings.size());
            for (Map.Entry<UUID, Station> entry : bindings.entrySet()) {
                player.sendMessage(UiTheme.MUTED + "- " + entry.getValue().name() + UiTheme.DISABLED + " | " + entry.getKey());
            }
            return true;
        }

        usage(player);
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(UiTheme.TEXT + "Void Crown Casino NPCs");
        player.sendMessage(UiTheme.WARNING + "/casinonpc bind <hub|coinflip|dice|lucky7|wheel|jackpot>");
        player.sendMessage(UiTheme.WARNING + "/casinonpc unbind");
        player.sendMessage(UiTheme.WARNING + "/casinonpc info");
        player.sendMessage(UiTheme.WARNING + "/casinonpc list");
    }

    private Villager nearestVillager(Player player, double radius) {
        Villager nearest = null;
        double best = radius * radius;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Villager)) continue;
            double distance = entity.getLocation().distanceSquared(player.getLocation());
            if (distance < best) {
                best = distance;
                nearest = (Villager) entity;
            }
        }
        return nearest;
    }

    private Station parse(String value) {
        if (value == null) return null;
        String n = value.trim().toLowerCase(Locale.ROOT);
        if ("hub".equals(n) || "reception".equals(n)) return Station.HUB;
        if ("coinflip".equals(n) || "coin-flip".equals(n) || "flip".equals(n)) return Station.COINFLIP;
        if ("dice".equals(n) || "crowndice".equals(n) || "crown-dice".equals(n)) return Station.DICE;
        if ("lucky7".equals(n) || "lucky-7".equals(n) || "seven".equals(n)) return Station.LUCKY7;
        if ("wheel".equals(n) || "fortune".equals(n)) return Station.WHEEL;
        if ("jackpot".equals(n) || "pot".equals(n)) return Station.JACKPOT;
        return null;
    }

    private String key(Station station) {
        switch (station) {
            case COINFLIP: return "coinflip";
            case DICE: return "dice";
            case LUCKY7: return "lucky7";
            case WHEEL: return "wheel";
            case JACKPOT: return "jackpot";
            default: return "hub";
        }
    }

    private String displayName(Station station) {
        switch (station) {
            case COINFLIP: return ChatColor.GOLD + "Coin Flip Dealer";
            case DICE: return ChatColor.RED + "Crown Dice Dealer";
            case LUCKY7: return ChatColor.LIGHT_PURPLE + "Lucky 7 Dealer";
            case WHEEL: return ChatColor.AQUA + "Wheel Master";
            case JACKPOT: return ChatColor.GREEN + "Jackpot Host";
            default: return ChatColor.GOLD.toString() + ChatColor.BOLD + "Void Crown Reception";
        }
    }

    private void load() {
        bindings.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("stations") == null) return;
        for (String key : yaml.getConfigurationSection("stations").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Station station = Station.valueOf(yaml.getString("stations." + key + ".type", "").toUpperCase(Locale.ROOT));
                bindings.put(uuid, station);
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Ungueltige Casino-NPC-Bindung in casino-npcs.yml: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Station> entry : bindings.entrySet()) {
            yaml.set("stations." + entry.getKey() + ".type", entry.getValue().name());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("casino-npcs.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("bind", "unbind", "info", "list"), args[0]);
        if (args.length == 2 && "bind".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("hub", "coinflip", "dice", "lucky7", "wheel", "jackpot"), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String value : values) if (value.startsWith(p)) result.add(value);
        return result;
    }
}
