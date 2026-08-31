package net.skykings.core.shop;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.bukkit.plugin.Plugin;
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

    private enum Type { SYSTEM, PVP_RESTOCK, BLACKSMITH, ENCHANT, RECYCLER, MERCHANT, JACKPOT }

    private final JavaPlugin plugin;
    private final SystemShopGui systemShopGui;
    private final PvpRestockShopGui pvpRestockShopGui;
    private BlacksmithShopGui blacksmithShopGui;
    private EnchantShopGui enchantShopGui;
    private BottleRecyclerGui bottleRecyclerGui;
    private TravelingMerchantGui travelingMerchantGui;
    private JackpotGui jackpotGui;
    private final File file;
    private final Map<UUID, Type> bindings = new LinkedHashMap<UUID, Type>();

    public ShopNpcService(JavaPlugin plugin, SystemShopGui systemShopGui, PvpRestockShopGui pvpRestockShopGui) {
        this.plugin = plugin;
        this.systemShopGui = systemShopGui;
        this.pvpRestockShopGui = pvpRestockShopGui;
        this.file = new File(plugin.getDataFolder(), "shop-npcs.yml");
        load();
    }

    public ShopNpcService(JavaPlugin plugin, SystemShopGui systemShopGui, PvpRestockShopGui pvpRestockShopGui,
                          BlacksmithShopGui blacksmithShopGui, EnchantShopGui enchantShopGui) {
        this(plugin, systemShopGui, pvpRestockShopGui);
        this.blacksmithShopGui = blacksmithShopGui;
        this.enchantShopGui = enchantShopGui;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        Type type = bindings.get(event.getRightClicked().getUniqueId());
        if (type == null) return;
        event.setCancelled(true);

        if (!isAllowedAtConfiguredLandmark(type, event.getRightClicked().getLocation())) {
            event.getPlayer().sendMessage(UiTheme.DANGER + "Dieser Haendler steht nicht in seinem vorgesehenen Bereich.");
            SoundFeedback.error(event.getPlayer());
            return;
        }

        if (!ensureExtendedShops()) {
            event.getPlayer().sendMessage(UiTheme.DANGER + "Shop-Service noch nicht bereit.");
            SoundFeedback.error(event.getPlayer());
            return;
        }

        switch (type) {
            case SYSTEM: systemShopGui.open(event.getPlayer()); break;
            case PVP_RESTOCK: pvpRestockShopGui.open(event.getPlayer()); break;
            case BLACKSMITH: blacksmithShopGui.open(event.getPlayer()); break;
            case ENCHANT: enchantShopGui.open(event.getPlayer()); break;
            case RECYCLER: bottleRecyclerGui.open(event.getPlayer()); break;
            case MERCHANT: travelingMerchantGui.open(event.getPlayer()); break;
            case JACKPOT: jackpotGui.open(event.getPlayer()); break;
            default: break;
        }
    }

    private boolean ensureExtendedShops() {
        if (blacksmithShopGui != null && enchantShopGui != null && bottleRecyclerGui != null
                && travelingMerchantGui != null && jackpotGui != null) return true;
        SkyKingsCoreAPI api = Bukkit.getServicesManager().load(SkyKingsCoreAPI.class);
        if (api == null) return false;
        if (blacksmithShopGui == null) blacksmithShopGui = new BlacksmithShopGui(api.getGuiManager(), api.getEconomyService());
        if (enchantShopGui == null) enchantShopGui = new EnchantShopGui(api.getGuiManager(), api.getShopTransactionService());
        if (bottleRecyclerGui == null) bottleRecyclerGui = new BottleRecyclerGui(api.getGuiManager(), api.getEconomyService());
        if (travelingMerchantGui == null) travelingMerchantGui = new TravelingMerchantGui(plugin, api.getGuiManager(), api.getShopTransactionService());
        if (jackpotGui == null) jackpotGui = new JackpotGui(plugin, api.getGuiManager(), api.getEconomyService());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(UiTheme.DANGER + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("bind".equals(sub)) {
            if (args.length < 2) {
                player.sendMessage(UiTheme.TEXT + "Shop-NPC binden");
                player.sendMessage(UiTheme.WARNING + "/shopnpc bind <system|pvp|blacksmith|enchant|recycler|merchant|jackpot>");
                return true;
            }
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(UiTheme.DANGER + "Kein Villager innerhalb von 6 Bloecken gefunden.");
                return true;
            }
            Type type = parse(args[1]);
            if (type == null) {
                player.sendMessage(UiTheme.DANGER + "Unbekannter Shoptyp.");
                return true;
            }
            if (!isAllowedAtConfiguredLandmark(type, villager.getLocation())) {
                player.sendMessage(UiTheme.DANGER + "Dieser Shop muss innerhalb seiner konfigurierten Map-Insel stehen.");
                return true;
            }
            bindings.put(villager.getUniqueId(), type);
            save();
            villager.setCustomName(displayName(type));
            villager.setCustomNameVisible(true);
            player.sendMessage(UiTheme.SUCCESS + "Shop-NPC gebunden.");
            SoundFeedback.success(player);
            return true;
        }

        if ("unbind".equals(sub) || "remove".equals(sub)) {
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(UiTheme.DANGER + "Kein Villager innerhalb von 6 Bloecken gefunden.");
                return true;
            }
            Type removed = bindings.remove(villager.getUniqueId());
            if (removed == null) {
                player.sendMessage(UiTheme.DANGER + "Dieser Villager ist kein SkyKings-Shop.");
                return true;
            }
            save();
            player.sendMessage(UiTheme.WARNING + "Shop-Bindung entfernt.");
            SoundFeedback.back(player);
            return true;
        }

        if ("info".equals(sub)) {
            Villager villager = nearestVillager(player, 6D);
            if (villager == null) {
                player.sendMessage(UiTheme.DANGER + "Kein Villager innerhalb von 6 Bloecken gefunden.");
                return true;
            }
            Type type = bindings.get(villager.getUniqueId());
            player.sendMessage(UiTheme.TEXT + "Shop-NPC");
            player.sendMessage(UiTheme.MUTED + "Status " + (type == null ? UiTheme.DANGER + "nicht gebunden" : UiTheme.SUCCESS + type.name()));
            return true;
        }

        sendUsage(player);
        return true;
    }

    private boolean isAllowedAtConfiguredLandmark(Type type, Location location) {
        String landmark = null;
        if (type == Type.BLACKSMITH) landmark = "blacksmith";
        else if (type == Type.MERCHANT) landmark = "merchant";
        if (landmark == null) return true;

        Plugin combat = Bukkit.getPluginManager().getPlugin("SkyKings-Combat");
        if (combat == null) return true;
        File landmarks = new File(combat.getDataFolder(), "map-landmarks.yml");
        if (!landmarks.exists()) return true;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(landmarks);
        String base = "landmarks." + landmark;
        String world = yaml.getString(base + ".world");
        if (world == null || world.trim().isEmpty()) return true;
        if (location == null || location.getWorld() == null || !world.equals(location.getWorld().getName())) return false;
        double dx = location.getX() - yaml.getDouble(base + ".x");
        double dy = location.getY() - yaml.getDouble(base + ".y");
        double dz = location.getZ() - yaml.getDouble(base + ".z");
        double radius = yaml.getDouble(base + ".radius", 8D);
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private String displayName(Type type) {
        switch (type) {
            case SYSTEM: return ChatColor.GOLD + "System Market";
            case PVP_RESTOCK: return ChatColor.LIGHT_PURPLE + "PvP Restock";
            case BLACKSMITH: return ChatColor.DARK_GRAY + "Blacksmith";
            case ENCHANT: return ChatColor.DARK_PURPLE + "Enchanter";
            case RECYCLER: return ChatColor.AQUA + "Bottle Recycler";
            case MERCHANT: return ChatColor.AQUA + "Black Market";
            case JACKPOT: return ChatColor.GOLD + "Jackpot";
            default: return ChatColor.GOLD + "Market";
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(UiTheme.TEXT + "Shop-NPCs");
        player.sendMessage(UiTheme.WARNING + "/shopnpc bind <Typ>");
        player.sendMessage(UiTheme.WARNING + "/shopnpc unbind");
        player.sendMessage(UiTheme.WARNING + "/shopnpc info");
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
        if ("recycler".equals(n) || "bottle".equals(n) || "bottles".equals(n)) return Type.RECYCLER;
        if ("merchant".equals(n) || "blackmarket".equals(n) || "black-market".equals(n)) return Type.MERCHANT;
        if ("jackpot".equals(n) || "pot".equals(n)) return Type.JACKPOT;
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
                plugin.getLogger().warning("Ungueltige Shop-NPC-Bindung in shop-npcs.yml: " + key);
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
        if (args.length == 2 && "bind".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("system", "pvp", "blacksmith", "enchant", "recycler", "merchant", "jackpot"), args[1]);
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
