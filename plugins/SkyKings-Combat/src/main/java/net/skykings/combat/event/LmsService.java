package net.skykings.combat.event;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.item.SkyKingsCurrencyItems;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Last-Man-Standing mit Queue-Lobby, Event-Isolation und einheitlichem SkyKings UI. */
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
    /** Spieler, die auf dem Death-Screen liegen, werden erst ueber PlayerRespawnEvent sicher zurueckgesetzt. */
    private final Map<UUID, Location> pendingRespawns = new LinkedHashMap<UUID, Location>();

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
            if (sender instanceof Player) open((Player) sender);
            else sender.sendMessage("LMS: " + (running ? "LIVE" : "WAITING") + " | Queue: " + queue.size() + " | Alive: " + alive.size());
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("join".equals(sub)) {
            if (!(sender instanceof Player)) return true;
            join((Player) sender, true);
            return true;
        }
        if ("leave".equals(sub)) {
            if (!(sender instanceof Player)) return true;
            leave((Player) sender, true);
            return true;
        }
        if ("start".equals(sub)) {
            if (!sender.hasPermission("skykings.admin.event")) { sender.sendMessage(UiTheme.DANGER + "Keine Berechtigung."); return true; }
            if (args.length < 2) { sender.sendMessage(UiTheme.WARNING + "/lms start <Arena>"); return true; }
            start(sender, args[1]);
            return true;
        }
        if ("stop".equals(sub)) {
            if (!sender.hasPermission("skykings.admin.event")) { sender.sendMessage(UiTheme.DANGER + "Keine Berechtigung."); return true; }
            stop(UiTheme.DANGER + "LMS wurde von Staff beendet.");
            return true;
        }
        if (sender instanceof Player) open((Player) sender);
        return true;
    }

    private void open(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Last Man Standing"), 27);
        String status = running ? UiTheme.DANGER + "LIVE" : UiTheme.SUCCESS + "READY";
        gui.setItem(4, UiItems.item(Material.DIAMOND_SWORD,
                UiTheme.PRIMARY + "Last Man Standing",
                UiTheme.MUTED + "Status " + status,
                UiTheme.MUTED + "Queue " + UiTheme.TEXT + queue.size(),
                UiTheme.MUTED + "Alive " + UiTheme.TEXT + alive.size()));

        boolean queued = queue.contains(player.getUniqueId());
        gui.setItem(11, UiItems.item(queued ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK,
                queued ? UiTheme.DANGER + "Queue verlassen" : UiTheme.SUCCESS + "Queue beitreten",
                running ? UiTheme.DANGER + "Event laeuft bereits"
                        : queued ? UiTheme.MUTED + "Du wartest auf den Start." : UiTheme.MUTED + "Melde dich fuer das naechste LMS an.",
                "",
                running ? UiTheme.DISABLED + "LOCKED" : UiItems.action("Klicken")), (p,e,s) -> {
            if (running) { SoundFeedback.error(p); return; }
            if (queue.contains(p.getUniqueId())) leave(p, false); else join(p, false);
            open(p);
        });

        gui.setItem(13, UiItems.item(Material.NETHER_STAR,
                UiTheme.LEGENDARY + "Reward",
                UiTheme.TEXT + UiFormat.coins(WIN_COINS),
                UiTheme.TEXT.toString() + WIN_STARS + UiTheme.MUTED + " SkyKings Sterne",
                "",
                UiTheme.MUTED + "Nur der letzte Ueberlebende gewinnt."));

        gui.setItem(15, UiItems.item(Material.BOOK,
                UiTheme.TEXT + "Regeln",
                UiTheme.MUTED + "Eigener Event-Fight ohne Open-World-Stats.",
                UiTheme.MUTED + "Tod oder Quit = ausgeschieden.",
                UiTheme.MUTED + "Event-Kills geben keine normalen Bounties."));

        if (player.hasPermission("skykings.admin.event")) {
            gui.setItem(22, UiItems.item(Material.COMMAND,
                    UiTheme.WARNING + "Staff Control",
                    UiTheme.MUTED + "Start " + UiTheme.TEXT + "/lms start <Arena>",
                    UiTheme.MUTED + "Stop " + UiTheme.TEXT + "/lms stop"));
        } else {
            gui.setItem(22, UiItems.item(Material.WATCH,
                    UiTheme.TEXT + "Event Lobby",
                    UiTheme.MUTED + (running ? "Match ist aktuell live." : "Warte auf einen Staff- oder Auto-Start.")));
        }
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void join(Player player, boolean message) {
        if (running) { error(player, "Das LMS laeuft bereits."); return; }
        if (participation.isInEvent(player.getUniqueId())) { error(player, "Du bist bereits in einem Event."); return; }
        if (queue.add(player.getUniqueId())) {
            if (message) player.sendMessage(UiTheme.SUCCESS + "LMS Queue beigetreten");
            Bukkit.broadcastMessage(UiTheme.PRIMARY + "LMS " + UiTheme.TEXT + player.getName()
                    + UiTheme.MUTED + " ist beigetreten • " + UiTheme.TEXT + queue.size() + UiTheme.MUTED + " in Queue");
            SoundFeedback.success(player);
        } else if (message) player.sendMessage(UiTheme.MUTED + "Du bist bereits in der LMS Queue.");
    }

    private void leave(Player player, boolean message) {
        if (running && alive.contains(player.getUniqueId())) {
            if (message) player.sendMessage(UiTheme.WARNING + "Verlassen gilt als Aufgabe.");
            eliminate(player.getUniqueId(), true);
        } else if (queue.remove(player.getUniqueId())) {
            if (message) player.sendMessage(UiTheme.MUTED + "LMS Queue verlassen.");
            SoundFeedback.back(player);
        } else if (message) player.sendMessage(UiTheme.MUTED + "Du bist nicht in der LMS Queue.");
    }

    private void start(CommandSender sender, String arenaRaw) {
        if (running) { sender.sendMessage(UiTheme.DANGER + "Es laeuft bereits ein LMS."); return; }
        String arena = arenaRaw.toLowerCase(Locale.ROOT);
        int spawnCount = arenas.countPrefix(arena, "spawn");
        if (!arenas.isReadyForLms(arena)) {
            sender.sendMessage(UiTheme.DANGER + "Arena nicht bereit.");
            sender.sendMessage(UiTheme.MUTED + "Benoetigt lobby + mindestens spawn1 bis spawn4.");
            return;
        }
        List<Player> players = new ArrayList<Player>();
        for (UUID uuid : new ArrayList<UUID>(queue)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !participation.isInEvent(uuid)) players.add(player);
        }
        if (players.size() < 2) { sender.sendMessage(UiTheme.DANGER + "Mindestens 2 Online-Spieler in der Queue noetig."); return; }
        if (players.size() > spawnCount) { sender.sendMessage(UiTheme.DANGER + "Zu viele Spieler fuer die Arena: " + players.size() + "/" + spawnCount); return; }

        running = true;
        activeArena = arena;
        sessionId = "lms-" + System.currentTimeMillis();
        alive.clear();
        returnLocations.clear();
        pendingRespawns.clear();
        queue.clear();

        int index = 1;
        for (Player player : players) {
            Location spawn = arenas.get(arena, "spawn" + index++);
            if (spawn == null) { stop(UiTheme.DANGER + "LMS Start abgebrochen: Spawnpunkt fehlt."); return; }
            UUID uuid = player.getUniqueId();
            if (!participation.join(uuid, EventParticipationService.Type.LMS, sessionId)) continue;
            alive.add(uuid);
            returnLocations.put(uuid, player.getLocation().clone());
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.teleport(spawn);
            SoundFeedback.levelUp(player);
        }
        Bukkit.broadcastMessage(UiTheme.PRIMARY + "LMS gestartet");
        Bukkit.broadcastMessage(UiTheme.TEXT.toString() + alive.size() + UiTheme.MUTED + " Spieler • Letzter Ueberlebender gewinnt");
        if (alive.size() < 2) stop(UiTheme.DANGER + "LMS konnte nicht mit genug Spielern gestartet werden.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        EventParticipationService.Participation p = participation.get(uuid);
        if (!running || p == null || p.getType() != EventParticipationService.Type.LMS || !sessionId.equals(p.getSessionId())) return;
        // LMS darf niemals das normale Spieler-Inventar vernichten. Ohne keepInventory wuerde das
        // Leeren der Drops auf 1.8 die Items dauerhaft loeschen.
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepLevel(true);
        eliminate(uuid, false);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location back = pendingRespawns.remove(event.getPlayer().getUniqueId());
        if (back != null) event.setRespawnLocation(back);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final Location back = pendingRespawns.remove(uuid);
        if (back != null) Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !player.isDead()) player.teleport(back);
            else if (player != null && player.isOnline()) pendingRespawns.put(uuid, back);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        queue.remove(uuid);
        if (running && alive.contains(uuid)) eliminate(uuid, true);
    }

    private void eliminate(UUID uuid, boolean forfeit) {
        if (!alive.remove(uuid)) return;
        participation.leave(uuid);
        Player player = Bukkit.getPlayer(uuid);
        Location back = returnLocations.remove(uuid);
        if (player != null && player.isOnline()) {
            if (back != null) {
                if (player.isDead()) pendingRespawns.put(uuid, back);
                else player.teleport(back);
            }
            player.sendMessage(UiTheme.DANGER + (forfeit ? "LMS aufgegeben." : "Aus dem LMS ausgeschieden."));
            SoundFeedback.error(player);
        } else if (back != null) {
            // Quit waehrend Death-Screen: Position fuer den naechsten Join/Respawn behalten.
            pendingRespawns.put(uuid, back);
        }
        Bukkit.broadcastMessage(UiTheme.PRIMARY + "LMS " + UiTheme.TEXT + name(uuid)
                + UiTheme.MUTED + " ausgeschieden • " + UiTheme.TEXT + alive.size() + UiTheme.MUTED + " verbleiben");
        if (alive.size() == 1) finishWinner(alive.iterator().next());
        else if (alive.isEmpty()) stop(UiTheme.WARNING + "LMS beendet • kein Gewinner");
    }

    private void finishWinner(UUID winner) {
        Player player = Bukkit.getPlayer(winner);
        economy.deposit(winner, WIN_COINS, "LMS_WIN", sessionId);
        if (player != null && player.isOnline()) {
            SkyKingsCurrencyItems.give(player, WIN_STARS);
            SoundFeedback.reward(player);
        }
        Bukkit.broadcastMessage(UiTheme.LEGENDARY + "LMS Winner " + UiTheme.TEXT + name(winner));
        Bukkit.broadcastMessage(UiTheme.MUTED + "Reward " + UiTheme.TEXT + UiFormat.coins(WIN_COINS)
                + UiTheme.MUTED + " • " + UiTheme.TEXT + WIN_STARS + UiTheme.MUTED + " SkyKings Sterne");
        stop(null);
    }

    private void stop(String broadcast) {
        for (UUID uuid : new ArrayList<UUID>(alive)) {
            participation.leave(uuid);
            Player player = Bukkit.getPlayer(uuid);
            Location back = returnLocations.remove(uuid);
            if (back == null) continue;
            if (player != null && player.isOnline()) {
                if (player.isDead()) pendingRespawns.put(uuid, back);
                else player.teleport(back);
            } else {
                pendingRespawns.put(uuid, back);
            }
        }
        alive.clear();
        returnLocations.clear();
        running = false;
        sessionId = null;
        activeArena = null;
        if (broadcast != null) Bukkit.broadcastMessage(broadcast);
    }

    public void shutdown() { stop(null); }

    private void error(Player player, String text) {
        player.sendMessage(UiTheme.DANGER + text);
        SoundFeedback.error(player);
    }

    private String name(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : uuid.toString().substring(0, 8);
    }
}
