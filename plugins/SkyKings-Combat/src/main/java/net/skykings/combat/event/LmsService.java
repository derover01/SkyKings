package net.skykings.combat.event;

import net.skykings.core.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Last-Man-Standing Serverevent. Queue und Match sind bewusst ohne Echtgeld/Wagers.
 * Event-Kills werden ueber EventParticipationService vom normalen SkyPvP isoliert.
 */
public final class LmsService implements Listener, CommandExecutor {

    private static final long WIN_COINS = 500_000L;
    private static final int WIN_STARS = 5;

    private final JavaPlugin plugin;
    private final EventArenaService arenas;
    private final EconomyService economy;
    private final EventParticipationService participation = EventParticipationService.global();
    private final Set<UUID> queue = new LinkedHashSet<UUID>();
    private final Set<UUID> alive = new LinkedHashSet<UUID>();
    private final Map<UUID, Location> returnLocations = new LinkedHashMap<UUID, Location>();

    private String sessionId;
    private String activeArena;
    private boolean running;

    public LmsService(JavaPlugin plugin, EventArenaService arenas, EconomyService economy) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "LMS: " + ChatColor.YELLOW + (running ? "laeuft" : "wartet")
                    + ChatColor.GRAY + " | Queue: " + queue.size() + " | Alive: " + alive.size());
            sender.sendMessage(ChatColor.YELLOW + "/lms join, /lms leave");
            if (sender.hasPermission("skykings.admin.event")) sender.sendMessage(ChatColor.YELLOW + "/lms start <Arena>, /lms stop");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("join".equals(sub)) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (running) { player.sendMessage(ChatColor.RED + "Das LMS laeuft bereits."); return true; }
            if (participation.isInEvent(player.getUniqueId())) { player.sendMessage(ChatColor.RED + "Du bist bereits in einem Event."); return true; }
            if (queue.add(player.getUniqueId())) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "[LMS] " + ChatColor.YELLOW + player.getName() + ChatColor.GRAY
                        + " ist beigetreten. " + queue.size() + " Spieler in der Queue.");
            } else player.sendMessage(ChatColor.YELLOW + "Du bist bereits in der LMS-Queue.");
            return true;
        }
        if ("leave".equals(sub)) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (running && alive.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Im laufenden LMS gilt Verlassen als Aufgabe.");
                eliminate(player.getUniqueId(), true);
            } else if (queue.remove(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "Du hast die LMS-Queue verlassen.");
            } else player.sendMessage(ChatColor.GRAY + "Du bist nicht in der LMS-Queue.");
            return true;
        }
        if ("start".equals(sub)) {
            if (!sender.hasPermission("skykings.admin.event")) { sender.sendMessage(ChatColor.RED + "Keine Berechtigung."); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Nutze /lms start <Arena>."); return true; }
            start(sender, args[1]);
            return true;
        }
        if ("stop".equals(sub)) {
            if (!sender.hasPermission("skykings.admin.event")) { sender.sendMessage(ChatColor.RED + "Keine Berechtigung."); return true; }
            stop(ChatColor.RED + "LMS wurde von Staff beendet.");
            return true;
        }
        return true;
    }

    private void start(CommandSender sender, String arenaRaw) {
        if (running) { sender.sendMessage(ChatColor.RED + "Es laeuft bereits ein LMS."); return; }
        String arena = arenaRaw.toLowerCase(Locale.ROOT);
        int spawnCount = arenas.countPrefix(arena, "spawn");
        if (!arenas.isReadyForLms(arena)) {
            sender.sendMessage(ChatColor.RED + "Arena nicht bereit: benoetigt lobby + mindestens spawn1 bis spawn4.");
            return;
        }
        List<Player> players = new ArrayList<Player>();
        for (UUID uuid : new ArrayList<UUID>(queue)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !participation.isInEvent(uuid)) players.add(player);
        }
        if (players.size() < 2) { sender.sendMessage(ChatColor.RED + "Mindestens 2 Online-Spieler in der Queue noetig."); return; }
        if (players.size() > spawnCount) { sender.sendMessage(ChatColor.RED + "Zu viele Spieler fuer die Arena-Spawns: " + players.size() + "/" + spawnCount); return; }

        running = true;
        activeArena = arena;
        sessionId = "lms-" + System.currentTimeMillis();
        alive.clear();
        returnLocations.clear();
        queue.clear();

        int index = 1;
        for (Player player : players) {
            Location spawn = arenas.get(arena, "spawn" + index++);
            if (spawn == null) { stop(ChatColor.RED + "LMS-Start abgebrochen: Spawnpunkt fehlt."); return; }
            UUID uuid = player.getUniqueId();
            if (!participation.join(uuid, EventParticipationService.Type.LMS, sessionId)) continue;
            alive.add(uuid);
            returnLocations.put(uuid, player.getLocation().clone());
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.teleport(spawn);
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7F, 1.3F);
        }
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "LMS START " + ChatColor.YELLOW
                + alive.size() + ChatColor.GRAY + " Spieler kaempfen. Letzter Ueberlebender gewinnt!");
        if (alive.size() < 2) stop(ChatColor.RED + "LMS konnte nicht mit genug Spielern gestartet werden.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        EventParticipationService.Participation p = participation.get(uuid);
        if (!running || p == null || p.getType() != EventParticipationService.Type.LMS || !sessionId.equals(p.getSessionId())) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepLevel(true);
        Bukkit.getScheduler().runTask(plugin, () -> eliminate(uuid, false));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        queue.remove(uuid);
        if (running && alive.contains(uuid)) Bukkit.getScheduler().runTask(plugin, () -> eliminate(uuid, true));
    }

    private void eliminate(UUID uuid, boolean forfeit) {
        if (!alive.remove(uuid)) return;
        participation.leave(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            Location back = returnLocations.get(uuid);
            if (back != null) player.teleport(back);
            player.sendMessage(ChatColor.RED + (forfeit ? "Du hast das LMS aufgegeben." : "Du wurdest aus dem LMS eliminiert."));
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "[LMS] " + ChatColor.YELLOW + name(uuid) + ChatColor.GRAY
                + " ist ausgeschieden. Noch " + alive.size() + " Spieler.");
        if (alive.size() == 1) finishWinner(alive.iterator().next());
        else if (alive.isEmpty()) stop(ChatColor.YELLOW + "LMS beendet - kein Gewinner.");
    }

    private void finishWinner(UUID winner) {
        Player player = Bukkit.getPlayer(winner);
        economy.deposit(winner, WIN_COINS, "LMS_WIN", sessionId);
        if (player != null && player.isOnline()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, WIN_STARS));
            for (ItemStack stack : leftovers.values()) player.getWorld().dropItemNaturally(player.getLocation(), stack);
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1F, 1.6F);
        }
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "LMS GEWINNER: " + ChatColor.YELLOW + name(winner)
                + ChatColor.GRAY + " | +" + WIN_COINS + " Coins + " + WIN_STARS + " Nethersterne");
        stop(null);
    }

    private void stop(String broadcast) {
        for (UUID uuid : new ArrayList<UUID>(alive)) {
            participation.leave(uuid);
            Player player = Bukkit.getPlayer(uuid);
            Location back = returnLocations.get(uuid);
            if (player != null && player.isOnline() && back != null) player.teleport(back);
        }
        alive.clear();
        returnLocations.clear();
        running = false;
        sessionId = null;
        activeArena = null;
        if (broadcast != null) Bukkit.broadcastMessage(broadcast);
    }

    public void shutdown() { stop(null); }

    private String name(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : uuid.toString().substring(0, 8);
    }
}
