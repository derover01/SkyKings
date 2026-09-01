package net.skykings.core.shop.rent;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Persistente Marktstand-Mietflaechen fuer PlayerShops.
 * Staff definiert Cuboids; Spieler mieten sie fuer Coins. Nur der aktive Mieter darf darin bauen
 * und neue PlayerShops platzieren. Nach Ablauf wird Verkauf deaktiviert, der letzte Mieter darf
 * seinen bestehenden Shop jedoch noch verwalten/entfernen.
 */
public final class ShopRentService implements Listener, CommandExecutor, TabCompleter, ShopRentalAccess {
    public static final String ADMIN_PERMISSION = "skykings.admin.shoprents";
    private static final long DEFAULT_PRICE = 500_000L;
    private static final long DEFAULT_DURATION_HOURS = 24L;
    private static final long MAX_DURATION_HOURS = 168L;

    private static final class Booth {
        final String id;
        String world;
        int minX, minY, minZ, maxX, maxY, maxZ;
        long price;
        long durationMillis;
        UUID tenant;
        String tenantName;
        UUID previousTenant;
        long expiresAt;

        Booth(String id) { this.id = id; }

        boolean contains(Location location) {
            if (location == null || location.getWorld() == null || world == null || !world.equals(location.getWorld().getName())) return false;
            double x = location.getX(), y = location.getY(), z = location.getZ();
            return x >= minX && x <= maxX + 1D && y >= minY && y <= maxY + 1D && z >= minZ && z <= maxZ + 1D;
        }

        boolean active(long now) { return tenant != null && expiresAt > now; }
    }

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final GuiManager guiManager;
    private final File file;
    private final Map<String, Booth> booths = new LinkedHashMap<String, Booth>();
    private final Map<UUID, Location> pos1 = new LinkedHashMap<UUID, Location>();
    private final Map<UUID, Location> pos2 = new LinkedHashMap<UUID, Location>();

    public ShopRentService(JavaPlugin plugin, EconomyService economy, GuiManager guiManager) {
        this.plugin = plugin;
        this.economy = economy;
        this.guiManager = guiManager;
        this.file = new File(plugin.getDataFolder(), "shop-rentals.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::expireDue, 20L * 30L, 20L * 30L);
    }

    @Override
    public boolean hasActiveRental(UUID uuid, Location location) {
        if (uuid == null) return false;
        Booth booth = boothAt(location);
        if (booth == null) return false;
        expire(booth, System.currentTimeMillis(), false);
        return booth.active(System.currentTimeMillis()) && uuid.equals(booth.tenant);
    }

    @Override
    public boolean isCurrentOrPreviousTenant(UUID uuid, Location location) {
        if (uuid == null) return false;
        Booth booth = boothAt(location);
        if (booth == null) return false;
        expire(booth, System.currentTimeMillis(), false);
        return uuid.equals(booth.tenant) || uuid.equals(booth.previousTenant);
    }

    public boolean isConfiguredRental(Location location) { return boothAt(location) != null; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0]) || "gui".equalsIgnoreCase(args[0])) {
            open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("rent".equals(sub) || "mieten".equals(sub)) {
            if (args.length < 2) { player.sendMessage(UiTheme.WARNING + "/shoprent rent <Stand>"); return true; }
            rent(player, args[1]);
            return true;
        }
        if ("release".equals(sub) || "kuendigen".equals(sub) || "kündigen".equals(sub)) {
            if (args.length < 2) { player.sendMessage(UiTheme.WARNING + "/shoprent release <Stand>"); return true; }
            release(player, args[1]);
            return true;
        }
        if ("tp".equals(sub)) {
            if (args.length < 2) { player.sendMessage(UiTheme.WARNING + "/shoprent tp <Stand>"); return true; }
            teleport(player, args[1]);
            return true;
        }

        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(UiTheme.DANGER + "Keine Berechtigung.");
            return true;
        }
        if ("pos1".equals(sub) || "pos2".equals(sub)) {
            Location block = player.getLocation().getBlock().getLocation();
            if ("pos1".equals(sub)) pos1.put(player.getUniqueId(), block); else pos2.put(player.getUniqueId(), block);
            player.sendMessage(UiTheme.SUCCESS + sub.toUpperCase(Locale.ROOT) + UiTheme.MUTED + " gesetzt: "
                    + block.getBlockX() + ", " + block.getBlockY() + ", " + block.getBlockZ());
            return true;
        }
        if ("create".equals(sub)) {
            create(player, args);
            return true;
        }
        if ("remove".equals(sub) || "delete".equals(sub)) {
            if (args.length < 2) { player.sendMessage(UiTheme.WARNING + "/shoprent remove <Stand>"); return true; }
            Booth removed = booths.remove(normalize(args[1]));
            if (removed == null) player.sendMessage(UiTheme.DANGER + "Stand nicht gefunden.");
            else { save(); player.sendMessage(UiTheme.WARNING + "Mietstand " + removed.id + " entfernt."); }
            return true;
        }
        if ("list".equals(sub)) {
            player.sendMessage(UiTheme.PRIMARY + "Shop-Mietstaende" + UiTheme.MUTED + " • " + booths.size());
            for (Booth booth : sortedBooths()) {
                expire(booth, System.currentTimeMillis(), false);
                player.sendMessage(UiTheme.TEXT + booth.id + UiTheme.MUTED + " • "
                        + (booth.active(System.currentTimeMillis()) ? UiTheme.WARNING + booth.tenantName : UiTheme.SUCCESS + "FREI")
                        + UiTheme.MUTED + " • " + format(booth.price) + " Coins");
            }
            return true;
        }
        usage(player);
        return true;
    }

    private void open(Player player) {
        expireDue();
        GuiSession gui = GuiSession.create(player, UiTheme.title("Market Rentals"), 54);
        gui.setItem(4, UiItems.item(Material.EMERALD,
                UiTheme.PRIMARY + "MARKET RENTALS",
                UiTheme.MUTED + "Miete einen geschuetzten Marktstand.",
                UiTheme.MUTED + "PlayerShops sind dort waehrend der Miete aktiv."));

        List<Booth> list = sortedBooths();
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        for (int i = 0; i < Math.min(slots.length, list.size()); i++) {
            final Booth booth = list.get(i);
            boolean active = booth.active(System.currentTimeMillis());
            boolean mine = active && player.getUniqueId().equals(booth.tenant);
            Material icon = mine ? Material.EMERALD_BLOCK : active ? Material.REDSTONE_BLOCK : Material.CHEST;
            String status = mine ? UiTheme.SUCCESS + "DEIN STAND" : active ? UiTheme.DANGER + "VERMIETET" : UiTheme.STATUS_READY;
            gui.setItem(slots[i], UiItems.item(icon,
                    UiTheme.TEXT + booth.id.toUpperCase(Locale.ROOT),
                    UiTheme.MUTED + "Preis: " + UiTheme.WARNING + format(booth.price) + " Coins",
                    UiTheme.MUTED + "Dauer: " + UiTheme.TEXT + (booth.durationMillis / 3_600_000L) + "h",
                    active ? UiTheme.MUTED + "Mieter: " + UiTheme.TEXT + safeName(booth.tenantName) : UiTheme.MUTED + "Sofort verfuegbar",
                    active ? UiTheme.MUTED + "Rest: " + UiTheme.TEXT + remaining(booth) : "",
                    "",
                    status,
                    !active ? UiItems.action("Klicken zum Mieten") : mine ? UiItems.action("Klicken zum Teleport") : ChatColor.DARK_GRAY + "Nicht verfuegbar"),
                    (p,e,s) -> {
                        if (!booth.active(System.currentTimeMillis())) rent(p, booth.id);
                        else if (p.getUniqueId().equals(booth.tenant)) teleport(p, booth.id);
                        open(p);
                    });
        }
        gui.setItem(UiTheme.NAV_HOME, UiItems.home(), (p,e,s) -> Bukkit.dispatchCommand(p, "commands"));
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void rent(Player player, String rawId) {
        Booth booth = booths.get(normalize(rawId));
        if (booth == null) { player.sendMessage(UiTheme.DANGER + "Mietstand nicht gefunden."); return; }
        long now = System.currentTimeMillis();
        expire(booth, now, true);
        if (booth.active(now)) {
            player.sendMessage(UiTheme.DANGER + "Dieser Stand ist bereits vermietet.");
            SoundFeedback.error(player);
            return;
        }
        for (Booth other : booths.values()) {
            expire(other, now, false);
            if (other.active(now) && player.getUniqueId().equals(other.tenant)) {
                player.sendMessage(UiTheme.DANGER + "Du hast bereits einen aktiven Mietstand: " + other.id);
                return;
            }
        }
        if (!economy.withdraw(player.getUniqueId(), booth.price, "SHOP_RENT", "Mietstand " + booth.id)) {
            player.sendMessage(UiTheme.DANGER + "Dir fehlen Coins. Preis: " + format(booth.price));
            SoundFeedback.error(player);
            return;
        }
        booth.tenant = player.getUniqueId();
        booth.tenantName = player.getName();
        booth.previousTenant = player.getUniqueId();
        booth.expiresAt = now + booth.durationMillis;
        save();
        player.sendMessage(UiTheme.SUCCESS + "Mietstand " + booth.id + " gemietet" + UiTheme.MUTED + " • "
                + (booth.durationMillis / 3_600_000L) + "h");
        player.sendMessage(UiTheme.MUTED + "Hier darfst du bauen und dein Haendler-Ei platzieren.");
        SoundFeedback.reward(player);
    }

    private void release(Player player, String rawId) {
        Booth booth = booths.get(normalize(rawId));
        if (booth == null || booth.tenant == null || !player.getUniqueId().equals(booth.tenant)) {
            player.sendMessage(UiTheme.DANGER + "Das ist nicht dein aktiver Mietstand.");
            return;
        }
        booth.previousTenant = booth.tenant;
        booth.tenant = null;
        booth.tenantName = null;
        booth.expiresAt = 0L;
        save();
        player.sendMessage(UiTheme.WARNING + "Miete beendet. Es gibt keine anteilige Rueckerstattung.");
        player.sendMessage(UiTheme.MUTED + "Bestehende PlayerShops hier sind jetzt deaktiviert, koennen aber noch von dir geleert/entfernt werden.");
    }

    private void teleport(Player player, String rawId) {
        Booth booth = booths.get(normalize(rawId));
        if (booth == null) { player.sendMessage(UiTheme.DANGER + "Mietstand nicht gefunden."); return; }
        World world = Bukkit.getWorld(booth.world);
        if (world == null) { player.sendMessage(UiTheme.DANGER + "Welt ist nicht geladen."); return; }
        Location target = new Location(world, (booth.minX + booth.maxX + 1) / 2D, booth.minY + 1D, (booth.minZ + booth.maxZ + 1) / 2D);
        player.teleport(target);
    }

    private void create(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(UiTheme.WARNING + "/shoprent create <ID> [Preis] [Stunden]");
            return;
        }
        Location a = pos1.get(player.getUniqueId()), b = pos2.get(player.getUniqueId());
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            player.sendMessage(UiTheme.DANGER + "Setze zuerst /shoprent pos1 und /shoprent pos2 in derselben Welt.");
            return;
        }
        String id = normalize(args[1]);
        if (id.isEmpty() || booths.containsKey(id)) { player.sendMessage(UiTheme.DANGER + "Ungueltige oder bereits belegte ID."); return; }
        long price = DEFAULT_PRICE, hours = DEFAULT_DURATION_HOURS;
        try {
            if (args.length >= 3) price = Long.parseLong(args[2].replace(".", ""));
            if (args.length >= 4) hours = Long.parseLong(args[3]);
        } catch (NumberFormatException ex) {
            player.sendMessage(UiTheme.DANGER + "Preis/Stunden muessen Zahlen sein."); return;
        }
        if (price < 1L || hours < 1L || hours > MAX_DURATION_HOURS) {
            player.sendMessage(UiTheme.DANGER + "Preis > 0; Dauer 1-" + MAX_DURATION_HOURS + " Stunden."); return;
        }
        Booth booth = new Booth(id);
        booth.world = a.getWorld().getName();
        booth.minX = Math.min(a.getBlockX(), b.getBlockX()); booth.maxX = Math.max(a.getBlockX(), b.getBlockX());
        booth.minY = Math.min(a.getBlockY(), b.getBlockY()); booth.maxY = Math.max(a.getBlockY(), b.getBlockY());
        booth.minZ = Math.min(a.getBlockZ(), b.getBlockZ()); booth.maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
        booth.price = price; booth.durationMillis = hours * 3_600_000L;
        booths.put(id, booth); save();
        player.sendMessage(UiTheme.SUCCESS + "Mietstand " + id + " erstellt" + UiTheme.MUTED + " • " + format(price) + " Coins / " + hours + "h");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        protect(event.getPlayer(), event.getBlock().getLocation(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        protect(event.getPlayer(), event.getBlock().getLocation(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        protect(event.getPlayer(), event.getClickedBlock().getLocation(), () -> event.setCancelled(true));
    }

    private void protect(Player player, Location location, Runnable cancel) {
        Booth booth = boothAt(location);
        if (booth == null) return;
        if (player.hasPermission("skykings.admin.shoprents.bypass")) return;
        expire(booth, System.currentTimeMillis(), false);
        if (booth.active(System.currentTimeMillis()) && player.getUniqueId().equals(booth.tenant)) return;
        cancel.run();
        player.sendMessage(UiTheme.DANGER + (booth.active(System.currentTimeMillis())
                ? "Dieser Marktstand ist an " + safeName(booth.tenantName) + " vermietet."
                : "Dieser Marktstand ist aktuell nicht gemietet."));
    }

    private Booth boothAt(Location location) {
        if (location == null) return null;
        for (Booth booth : booths.values()) if (booth.contains(location)) return booth;
        return null;
    }

    private void expireDue() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        for (Booth booth : booths.values()) changed |= expire(booth, now, false);
        if (changed) save();
    }

    private boolean expire(Booth booth, long now, boolean persist) {
        if (booth == null || booth.tenant == null || booth.expiresAt <= 0L || booth.expiresAt > now) return false;
        booth.previousTenant = booth.tenant;
        booth.tenant = null;
        booth.tenantName = null;
        booth.expiresAt = 0L;
        if (persist) save();
        return true;
    }

    private List<Booth> sortedBooths() {
        List<Booth> list = new ArrayList<Booth>(booths.values());
        Collections.sort(list, Comparator.comparing(b -> b.id));
        return list;
    }

    private String remaining(Booth booth) {
        long seconds = Math.max(0L, (booth.expiresAt - System.currentTimeMillis() + 999L) / 1000L);
        long hours = seconds / 3600L, minutes = (seconds % 3600L) / 60L;
        return hours + "h " + minutes + "m";
    }

    private String normalize(String raw) { return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", ""); }
    private String safeName(String name) { return name == null || name.isEmpty() ? "Unbekannt" : name; }
    private String format(long value) { return String.format(Locale.GERMANY, "%,d", value); }

    private void usage(Player player) {
        player.sendMessage(UiTheme.TEXT + "Market Rentals");
        player.sendMessage(UiTheme.WARNING + "/shoprent" + UiTheme.MUTED + " - Uebersicht");
        player.sendMessage(UiTheme.WARNING + "/shoprent rent <Stand>");
        player.sendMessage(UiTheme.WARNING + "/shoprent release <Stand>");
        player.sendMessage(UiTheme.WARNING + "/shoprent tp <Stand>");
        if (player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(UiTheme.DANGER + "Staff: /shoprent pos1 | pos2 | create <ID> [Preis] [Stunden] | remove <ID> | list");
        }
    }

    private void load() {
        booths.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("booths");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            String base = "booths." + id;
            Booth booth = new Booth(normalize(id));
            booth.world = yaml.getString(base + ".world");
            booth.minX = yaml.getInt(base + ".min.x"); booth.minY = yaml.getInt(base + ".min.y"); booth.minZ = yaml.getInt(base + ".min.z");
            booth.maxX = yaml.getInt(base + ".max.x"); booth.maxY = yaml.getInt(base + ".max.y"); booth.maxZ = yaml.getInt(base + ".max.z");
            booth.price = Math.max(1L, yaml.getLong(base + ".price", DEFAULT_PRICE));
            booth.durationMillis = Math.max(3_600_000L, yaml.getLong(base + ".duration-millis", DEFAULT_DURATION_HOURS * 3_600_000L));
            booth.tenant = uuid(yaml.getString(base + ".tenant"));
            booth.previousTenant = uuid(yaml.getString(base + ".previous-tenant"));
            booth.tenantName = yaml.getString(base + ".tenant-name");
            booth.expiresAt = yaml.getLong(base + ".expires-at", 0L);
            if (booth.world != null) booths.put(booth.id, booth);
        }
        expireDue();
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Booth booth : booths.values()) {
            String base = "booths." + booth.id;
            yaml.set(base + ".world", booth.world);
            yaml.set(base + ".min.x", booth.minX); yaml.set(base + ".min.y", booth.minY); yaml.set(base + ".min.z", booth.minZ);
            yaml.set(base + ".max.x", booth.maxX); yaml.set(base + ".max.y", booth.maxY); yaml.set(base + ".max.z", booth.maxZ);
            yaml.set(base + ".price", booth.price);
            yaml.set(base + ".duration-millis", booth.durationMillis);
            yaml.set(base + ".tenant", booth.tenant == null ? null : booth.tenant.toString());
            yaml.set(base + ".tenant-name", booth.tenantName);
            yaml.set(base + ".previous-tenant", booth.previousTenant == null ? null : booth.previousTenant.toString());
            yaml.set(base + ".expires-at", booth.expiresAt);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("shop-rentals.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private UUID uuid(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) { return null; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<String>(Arrays.asList("rent", "release", "tp"));
            if (sender.hasPermission(ADMIN_PERMISSION)) values.addAll(Arrays.asList("pos1", "pos2", "create", "remove", "list"));
            return filter(values, args[0]);
        }
        if (args.length == 2 && ("rent".equalsIgnoreCase(args[0]) || "release".equalsIgnoreCase(args[0])
                || "tp".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))) {
            return filter(new ArrayList<String>(booths.keySet()), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String rawPrefix) {
        String prefix = rawPrefix == null ? "" : rawPrefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(value);
        return out;
    }
}
