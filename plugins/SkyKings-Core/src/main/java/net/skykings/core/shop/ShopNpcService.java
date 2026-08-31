package net.skykings.core.shop;

import net.skykings.core.api.SkyKingsCoreAPI;
import org.bukkit.Bukkit;
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

/** Persistente Bindung existierender Villager an SkyKings-Shops. */
public final class ShopNpcService implements Listener, CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "skykings.admin.shopnpc";

    private enum Type { SYSTEM, PVP_RESTOCK, BLACKSMITH, ENCHANT }

    private final JavaPlugin plugin;
    private final SystemShopGui systemShopGui;
    private final PvpRestockShopGui pvpRestockShopGui;
    private final BlacksmithShopGui blacksmithShopGui;
    private final EnchantShopGui enchantShopGui;
    private final File file;
    private final Map<UUID, Type> bindings = new LinkedHashMap<UUID, Type>();

    public ShopNpcService(JavaPlugin plugin, SystemShopGui systemShopGui, PvpRestockShopGui pvpRestockShopGui) {
        SkyKingsCoreAPI api = Bukkit.getServicesManager().load(SkyKingsCoreAPI.class);
        if (api == null) throw new IllegalStateException("SkyKingsCoreAPI nicht verfügbar");
        this.plugin = plugin;
        this.systemShopGui = systemShopGui;
        this.pvpRestockShopGui = pvpRestockShopGui;
        this.blacksmithShopGui = new BlacksmithShopGui(api.getGuiManager(), api.getEconomyService());
        this.enchantShopGui = new EnchantShopGui(api.getGuiManager(), api.getShopTransactionService());
        this.file = new File(plugin.getDataFolder(), "shop-npcs.yml");
        load();
    }

    public ShopNpcService(JavaPlugin plugin, SystemShopGui systemShopGui, PvpRestockShopGui pvpRestockShopGui,
                          BlacksmithShopGui blacksmithShopGui, EnchantShopGui enchantShopGui) {
        this.plugin = plugin;
        this.systemShopGui = systemShopGui;
        this.pvpRestockShopGui = pvpRestockShopGui;
        this.blacksmithShopGui = blacksmithShopGui;
        this.enchantShopGui = enchantShopGui;
        this.file = new File(plugin.getDataFolder(), "shop-npcs.yml");
        load();
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        Type type = bindings.get(event.getRightClicked().getUniqueId());
        if (type == null) return;
        event.setCancelled(true);
        switch (type) {
            case SYSTEM: systemShopGui.open(event.getPlayer()); break;
            case PVP_RESTOCK: pvpRestockShopGui.open(event.getPlayer()); break;
            case BLACKSMITH: blacksmithShopGui.open(event.getPlayer()); break;
            case ENCHANT: enchantShopGui.open(event.getPlayer()); break;
            default: break;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur für Spieler verfügbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("bind".equals(sub)) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Nutze /shopnpc bind <system|pvp|blacksmith|enchant>.");
                return true;
            }
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(ChatColor.RED + "Kein Villager innerhalb von 6 Blöcken gefunden.");
                return true;
            }
            Type type = parse(args[1]);
            if (type == null) {
                player.sendMessage(ChatColor.RED + "Unbekannter Shoptyp.");
                return true;
            }
            bindings.put(villager.getUniqueId(), type);
            save();
            villager.setCustomName(displayName(type));
            villager.setCustomNameVisible(true);
            player.sendMessage(ChatColor.GREEN + "Villager wurde als " + type.name() + " Shop gebunden.");
            return true;
        }

        if ("unbind".equals(sub) || "remove".equals(sub)) {
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(ChatColor.RED + "Kein Villager innerhalb von 6 Blöcken gefunden.");
                return true;
            }
            Type removed = bindings.remove(villager.getUniqueId());
            if (removed == null) {
                player.sendMessage(ChatColor.RED + "Dieser Villager ist kein SkyKings-Shop.");
                return true;
            }
            save();
            player.sendMessage(ChatColor.YELLOW + "Shop-Bindung entfernt.");
            return true;
        }

        if ("info".equals(sub)) {
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(ChatColor.RED + "Kein Villager innerhalb von 6 Blöcken gefunden.");
                return true;
            }
            Type type = bindings.get(villager.getUniqueId());
            player.sendMessage(ChatColor.GRAY + "Shop: " + (type == null ? ChatColor.RED + "nicht gebunden" : ChatColor.GREEN + type.name()));
            player.sendMessage(ChatColor.DARK_GRAY + "UUID: " + villager.getUniqueId());
            return true;
        }

        sendUsage(player);
        return true;
    }

    private String displayName(Type type) {
        switch (type) {
            case SYSTEM: return ChatColor.GOLD + "Systemhändler";
            case PVP_RESTOCK: return ChatColor.LIGHT_PURPLE + "PvP Restock";
            case BLACKSMITH: return ChatColor.DARK_GRAY + "Blacksmith";
            case ENCHANT: return ChatColor.DARK_PURPLE + "Enchanter";
            default: return ChatColor.GOLD + "Händler";
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "SkyKings Shop-NPCs");
        player.sendMessage(ChatColor.YELLOW + "/shopnpc bind system");
        player.sendMessage(ChatColor.YELLOW + "/shopnpc bind pvp");
        player.sendMessage(ChatColor.YELLOW + "/shopnpc bind blacksmith");
        player.sendMessage(ChatColor.YELLOW + "/shopnpc bind enchant");
        player.sendMessage(ChatColor.YELLOW + "/shopnpc unbind");
        player.sendMessage(ChatColor.YELLOW + "/shopnpc info");
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

    private Type parse(String value) {
        if (value == null) return null;
        String n = value.trim().toLowerCase(Locale.ROOT);
        if ("system".equals(n) || "coins".equals(n)) return Type.SYSTEM;
        if ("pvp".equals(n) || "restock".equals(n) || "netherstar".equals(n)) return Type.PVP_RESTOCK;
        if ("blacksmith".equals(n) || "smith".equals(n) || "repair".equals(n)) return Type.BLACKSMITH;
        if ("enchant".equals(n) || "enchanter".equals(n) || "level".equals(n)) return Type.ENCHANT;
        return null;
    }

    private void load() {
        bindings.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("shops") == null) return;
        for (String key : yaml.getConfigurationSection("shops").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Type type = Type.valueOf(yaml.getString("shops." + key + ".type", "").toUpperCase(Locale.ROOT));
                bindings.put(uuid, type);
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Ungültige Shop-NPC-Bindung in shop-npcs.yml: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Type> entry : bindings.entrySet()) yaml.set("shops." + entry.getKey() + ".type", entry.getValue().name());
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("shop-npcs.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("bind", "unbind", "info"), args[0]);
        if (args.length == 2 && "bind".equalsIgnoreCase(args[0])) return filter(Arrays.asList("system", "pvp", "blacksmith", "enchant"), args[1]);
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String value : values) if (value.startsWith(p)) result.add(value);
        return result;
    }
}
