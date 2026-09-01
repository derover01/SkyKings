package net.skykings.combat.event;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Kompakter Single-Arena-Tournament-Controller.
 * Eine Runde besteht aus nacheinander ausgespielten 1v1s; Sieger rutschen in die naechste Runde.
 */
public final class TournamentService implements Listener {
    public static final long WINNER_REWARD = 1_000_000L;

    private final JavaPlugin plugin;
    private final EventArenaService arenas;
    private final EconomyService economy;
    private final EventParticipationService participation = EventParticipationService.global();
    private final Set<UUID> queue = new LinkedHashSet<UUID>();
    private final Map<UUID, Location> returnLocations = new LinkedHashMap<UUID, Location>();
    private final Map<UUID, Location> pendingRespawns = new LinkedHashMap<UUID, Location>();

    private boolean running;
    private String sessionId;
    private List<UUID> currentRound = new ArrayList<UUID>();
    private List<UUID> nextRound = new ArrayList<UUID>();
    private int roundNumber;
    private int matchCursor;
    private UUID fighterA;
    private UUID fighterB;

    public TournamentService(JavaPlugin plugin, EventArenaService arenas, EconomyService economy) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.economy = economy;
    }

    public boolean join(Player player) {
        if (player == null || running || participation.isInEvent(player.getUniqueId())) return false;
        boolean added = queue.add(player.getUniqueId());
        if (added) {
            player.sendMessage(UiTheme.SUCCESS + "Tournament Queue beigetreten" + UiTheme.MUTED + " • " + queue.size() + " Spieler");
            SoundFeedback.success(player);
        }
        return added;
    }

    public boolean leave(Player player) {
        if (player == null || running) return false;
        boolean removed = queue.remove(player.getUniqueId());
        if (removed) player.sendMessage(UiTheme.MUTED + "Tournament Queue verlassen.");
        return removed;
    }

    public int queueSize() { return queue.size(); }
    public boolean isRunning() { return running; }
    public int getRoundNumber() { return roundNumber; }

    public boolean start() {
        if (running || queue.size() < 4 || !arenaReady()) return false;
        List<UUID> players = new ArrayList<UUID>(queue);
        queue.clear();
        Collections.shuffle(players);

        running = true;
        sessionId = "tournament-" + System.currentTimeMillis();
        currentRound = players;
        nextRound = new ArrayList<UUID>();
        roundNumber = 1;
        matchCursor = 0;
        fighterA = null;
        fighterB = null;

        Location lobby = arenas.get("tournament", "lobby");
        Location spectator = arenas.get("tournament", "spectator");
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            returnLocations.put(uuid, player.getLocation().clone());
            if (!participation.join(uuid, EventParticipationService.Type.TOURNAMENT, sessionId)) continue;
            Location target = lobby != null ? lobby : spectator;
            if (target != null) player.teleport(target);
        }
        broadcast(UiTheme.PRIMARY + "TOURNAMENT" + UiTheme.MUTED + " startet mit " + UiTheme.TEXT + players.size() + UiTheme.MUTED + " Spielern.");
        Bukkit.getScheduler().runTaskLater(plugin, this::startNextMatch, 40L);
        return true;
    }

    public void stop(boolean announce) {
        if (!running && queue.isEmpty()) return;
        if (announce) broadcast(UiTheme.DANGER + "Tournament wurde von Staff beendet.");
        for (UUID uuid : new ArrayList<UUID>(returnLocations.keySet())) restore(uuid);
        queue.clear();
        resetRuntime();
    }

    private void startNextMatch() {
        if (!running) return;

        // Runde beendet -> Siegerliste wird neue Runde.
        if (matchCursor >= currentRound.size()) {
            if (nextRound.size() == 1) {
                finishTournament(nextRound.get(0));
                return;
            }
            currentRound = new ArrayList<UUID>(nextRound);
            nextRound.clear();
            matchCursor = 0;
            roundNumber++;
            broadcast(UiTheme.PRIMARY + "TOURNAMENT" + UiTheme.MUTED + " • Runde " + UiTheme.TEXT + roundNumber);
        }

        // Freilos bei ungerader Spielerzahl.
        if (matchCursor == currentRound.size() - 1) {
            UUID bye = currentRound.get(matchCursor++);
            if (isOnlineParticipant(bye)) {
                nextRound.add(bye);
                Player player = Bukkit.getPlayer(bye);
                if (player != null) player.sendMessage(UiTheme.SUCCESS + "Freilos" + UiTheme.MUTED + " • Du bist eine Runde weiter.");
            }
            Bukkit.getScheduler().runTaskLater(plugin, this::startNextMatch, 20L);
            return;
        }

        fighterA = currentRound.get(matchCursor++);
        fighterB = currentRound.get(matchCursor++);
        Player a = Bukkit.getPlayer(fighterA);
        Player b = Bukkit.getPlayer(fighterB);
        if (a == null || !a.isOnline()) {
            advanceWithoutFight(fighterB);
            return;
        }
        if (b == null || !b.isOnline()) {
            advanceWithoutFight(fighterA);
            return;
        }

        Location aSpawn = arenas.get("tournament", "a");
        Location bSpawn = arenas.get("tournament", "b");
        if (aSpawn == null || bSpawn == null) {
            stop(true);
            return;
        }
        prepare(a); prepare(b);
        a.teleport(aSpawn);
        b.teleport(bSpawn);
        a.sendMessage(UiTheme.PRIMARY + "MATCH" + UiTheme.MUTED + " gegen " + UiTheme.TEXT + b.getName());
        b.sendMessage(UiTheme.PRIMARY + "MATCH" + UiTheme.MUTED + " gegen " + UiTheme.TEXT + a.getName());
        broadcast(UiTheme.MUTED + "Match: " + UiTheme.TEXT + a.getName() + UiTheme.MUTED + " vs " + UiTheme.TEXT + b.getName());
        SoundFeedback.notify(a); SoundFeedback.notify(b);
    }

    private void advanceWithoutFight(UUID winner) {
        if (winner != null && isOnlineParticipant(winner)) nextRound.add(winner);
        fighterA = null; fighterB = null;
        Bukkit.getScheduler().runTaskLater(plugin, this::startNextMatch, 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        if (!running) return;
        UUID loser = event.getEntity().getUniqueId();
        if (!loser.equals(fighterA) && !loser.equals(fighterB)) return;

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
        event.setDeathMessage(null);

        UUID winner = loser.equals(fighterA) ? fighterB : fighterA;
        nextRound.add(winner);
        fighterA = null; fighterB = null;

        Location back = returnLocations.get(loser);
        if (back != null) pendingRespawns.put(loser, back);
        participation.leave(loser);

        Player winnerPlayer = Bukkit.getPlayer(winner);
        if (winnerPlayer != null && winnerPlayer.isOnline()) {
            prepare(winnerPlayer);
            Location lobby = arenas.get("tournament", "lobby");
            Location spectator = arenas.get("tournament", "spectator");
            if (lobby != null) winnerPlayer.teleport(lobby);
            else if (spectator != null) winnerPlayer.teleport(spectator);
            winnerPlayer.sendMessage(UiTheme.SUCCESS + "Match gewonnen" + UiTheme.MUTED + " • Naechste Runde erreicht.");
            SoundFeedback.reward(winnerPlayer);
        }
        event.getEntity().sendMessage(UiTheme.DANGER + "Aus dem Tournament ausgeschieden.");
        Bukkit.getScheduler().runTaskLater(plugin, this::startNextMatch, 50L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Location back = pendingRespawns.remove(event.getPlayer().getUniqueId());
        if (back != null) event.setRespawnLocation(back);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        queue.remove(uuid);
        if (!running || !participation.isInEvent(uuid)) return;
        if (uuid.equals(fighterA) || uuid.equals(fighterB)) {
            UUID winner = uuid.equals(fighterA) ? fighterB : fighterA;
            participation.leave(uuid);
            returnLocations.remove(uuid);
            nextRound.add(winner);
            fighterA = null; fighterB = null;
            Bukkit.getScheduler().runTaskLater(plugin, this::startNextMatch, 20L);
            return;
        }
        participation.leave(uuid);
        currentRound.remove(uuid);
        nextRound.remove(uuid);
        returnLocations.remove(uuid);
    }

    private void finishTournament(UUID winnerId) {
        Player winner = Bukkit.getPlayer(winnerId);
        if (winner != null && winner.isOnline()) {
            economy.deposit(winnerId, WINNER_REWARD, "TOURNAMENT_WIN", "SkyKings Tournament");
            winner.sendMessage(UiTheme.LEGENDARY + "TOURNAMENT CHAMPION" + UiTheme.MUTED + " • +1.000.000 Coins");
            SoundFeedback.reward(winner);
        }
        String winnerName = winner != null ? winner.getName() : "Unbekannt";
        broadcast(UiTheme.LEGENDARY + "TOURNAMENT CHAMPION " + UiTheme.TEXT + winnerName);
        for (UUID uuid : new ArrayList<UUID>(returnLocations.keySet())) restore(uuid);
        resetRuntime();
    }

    private void restore(UUID uuid) {
        participation.leave(uuid);
        Player player = Bukkit.getPlayer(uuid);
        Location back = returnLocations.remove(uuid);
        if (player != null && player.isOnline() && back != null) {
            player.teleport(back);
            prepare(player);
        }
    }

    private boolean arenaReady() {
        return arenas.get("tournament", "a") != null
                && arenas.get("tournament", "b") != null
                && (arenas.get("tournament", "lobby") != null || arenas.get("tournament", "spectator") != null);
    }

    private boolean isOnlineParticipant(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline() && participation.isSameSession(uuid, uuid);
    }

    private void prepare(Player player) {
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setHealth(Math.min(player.getMaxHealth(), 20D));
    }

    private void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(message);
    }

    private void resetRuntime() {
        for (UUID uuid : new ArrayList<UUID>(returnLocations.keySet())) participation.leave(uuid);
        returnLocations.clear();
        pendingRespawns.clear();
        currentRound.clear();
        nextRound.clear();
        running = false;
        sessionId = null;
        roundNumber = 0;
        matchCursor = 0;
        fighterA = null;
        fighterB = null;
    }

    public void shutdown() { stop(false); }
}
