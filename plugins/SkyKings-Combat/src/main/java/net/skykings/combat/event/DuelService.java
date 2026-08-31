package net.skykings.combat.event;

import net.skykings.combat.tag.CombatTagService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sicherer 1v1-Duel-Lifecycle ohne Wagers und ohne Inventar-Manipulation.
 * Die Spieler kaempfen mit ihrem aktuellen Gear, behalten ihr Inventar beim Tod und
 * Open-World-Stats/Rewards werden ueber EventParticipationService isoliert.
 */
public final class DuelService implements Listener {

    private static final long REQUEST_TIMEOUT_MILLIS = 30_000L;

    public enum StartResult {
        SUCCESS,
        ARENA_NOT_READY,
        PLAYER_BUSY,
        COMBAT_TAGGED,
        TELEPORT_FAILED
    }

    private static final class Request {
        final UUID requester;
        final String arena;
        final long expiresAt;
        Request(UUID requester, String arena, long expiresAt) {
            this.requester = requester;
            this.arena = arena;
            this.expiresAt = expiresAt;
        }
    }

    private static final class Session {
        final String id;
        final UUID first;
        final UUID second;
        final Location firstReturn;
        final Location secondReturn;
        boolean finished;

        Session(String id, UUID first, UUID second, Location firstReturn, Location secondReturn) {
            this.id = id;
            this.first = first;
            this.second = second;
            this.firstReturn = firstReturn;
            this.secondReturn = secondReturn;
        }

        UUID opponent(UUID player) { return first.equals(player) ? second : first; }
        Location returnFor(UUID player) { return first.equals(player) ? firstReturn : secondReturn; }
    }

    private final JavaPlugin plugin;
    private final EventArenaService arenas;
    private final CombatTagService combatTags;
    private final EventParticipationService participation = EventParticipationService.global();
    private final Map<UUID, Request> requests = new HashMap<UUID, Request>();
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final Map<UUID, Location> pendingRespawns = new HashMap<UUID, Location>();

    public DuelService(JavaPlugin plugin, EventArenaService arenas, CombatTagService combatTags) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.combatTags = combatTags;
    }

    public boolean request(Player requester, Player target, String arena) {
        if (requester == null || target == null || requester.equals(target)) return false;
        if (isBusy(requester.getUniqueId()) || isBusy(target.getUniqueId())) return false;
        String selectedArena = arena == null || arena.trim().isEmpty() ? "duel" : arena.trim().toLowerCase(java.util.Locale.ROOT);
        requests.put(target.getUniqueId(), new Request(requester.getUniqueId(), selectedArena,
                System.currentTimeMillis() + REQUEST_TIMEOUT_MILLIS));
        requester.sendMessage(ChatColor.GREEN + "Duel-Anfrage an " + target.getName() + " gesendet. Arena: " + selectedArena);
        target.sendMessage(ChatColor.GOLD + requester.getName() + ChatColor.YELLOW + " fordert dich zum Duel heraus.");
        target.sendMessage(ChatColor.GRAY + "/duel accept " + ChatColor.DARK_GRAY + "oder " + ChatColor.GRAY + "/duel deny");
        target.playSound(target.getLocation(), Sound.NOTE_PLING, 0.6F, 1.4F);
        return true;
    }

    public StartResult accept(Player target) {
        Request request = validRequest(target.getUniqueId());
        if (request == null) return StartResult.PLAYER_BUSY;
        requests.remove(target.getUniqueId());
        Player requester = Bukkit.getPlayer(request.requester);
        if (requester == null || !requester.isOnline()) return StartResult.PLAYER_BUSY;
        return start(requester, target, request.arena);
    }

    public boolean deny(Player target) {
        Request request = validRequest(target.getUniqueId());
        if (request == null) return false;
        requests.remove(target.getUniqueId());
        Player requester = Bukkit.getPlayer(request.requester);
        if (requester != null) requester.sendMessage(ChatColor.RED + target.getName() + " hat die Duel-Anfrage abgelehnt.");
        target.sendMessage(ChatColor.YELLOW + "Duel-Anfrage abgelehnt.");
        return true;
    }

    public boolean hasPendingRequest(UUID target) { return validRequest(target) != null; }
    public boolean isBusy(UUID uuid) { return participation.isInEvent(uuid) || sessions.containsKey(uuid); }

    private StartResult start(Player first, Player second, String arena) {
        if (isBusy(first.getUniqueId()) || isBusy(second.getUniqueId())) return StartResult.PLAYER_BUSY;
        if (combatTags.isTagged(first.getUniqueId()) || combatTags.isTagged(second.getUniqueId())) return StartResult.COMBAT_TAGGED;
        Location a = arenas.get(arena, "a");
        Location b = arenas.get(arena, "b");
        if (a == null || b == null) return StartResult.ARENA_NOT_READY;

        String id = UUID.randomUUID().toString();
        if (!participation.join(first.getUniqueId(), EventParticipationService.Type.DUEL, id)) return StartResult.PLAYER_BUSY;
        if (!participation.join(second.getUniqueId(), EventParticipationService.Type.DUEL, id)) {
            participation.leave(first.getUniqueId());
            return StartResult.PLAYER_BUSY;
        }

        Session session = new Session(id, first.getUniqueId(), second.getUniqueId(),
                first.getLocation().clone(), second.getLocation().clone());
        sessions.put(first.getUniqueId(), session);
        sessions.put(second.getUniqueId(), session);

        boolean firstTeleported = first.teleport(a);
        boolean secondTeleported = second.teleport(b);
        if (!firstTeleported || !secondTeleported) {
            rollbackStart(session, first, second);
            return StartResult.TELEPORT_FAILED;
        }

        prepare(first);
        prepare(second);
        first.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "DUEL " + ChatColor.YELLOW + "gegen " + second.getName());
        second.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "DUEL " + ChatColor.YELLOW + "gegen " + first.getName());
        first.playSound(first.getLocation(), Sound.NOTE_PLING, 0.7F, 1.2F);
        second.playSound(second.getLocation(), Sound.NOTE_PLING, 0.7F, 1.2F);
        return StartResult.SUCCESS;
    }

    private void rollbackStart(Session session, Player first, Player second) {
        sessions.remove(session.first);
        sessions.remove(session.second);
        participation.leave(session.first);
        participation.leave(session.second);
        if (first.isOnline()) first.teleport(session.firstReturn);
        if (second.isOnline()) second.teleport(session.secondReturn);
    }

    private void prepare(Player player) {
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setHealth(Math.min(player.getMaxHealth(), 20D));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        Session session = sessions.get(loser.getUniqueId());
        if (session == null || session.finished) return;
        EventParticipationService.Participation state = participation.get(loser.getUniqueId());
        if (state == null || state.getType() != EventParticipationService.Type.DUEL) return;

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
        event.setDeathMessage(null);

        Player winner = Bukkit.getPlayer(session.opponent(loser.getUniqueId()));
        finishByDeath(session, loser, winner);
    }

    private void finishByDeath(Session session, Player loser, Player winner) {
        if (session.finished) return;
        session.finished = true;
        sessions.remove(session.first);
        sessions.remove(session.second);

        Location loserReturn = session.returnFor(loser.getUniqueId());
        pendingRespawns.put(loser.getUniqueId(), loserReturn);
        // Loser bleibt bis nach PlayerRespawnEvent Event-Teilnehmer, damit weder StarterKit
        // noch Open-World-Listener in den Respawn eingreifen.

        if (winner != null && winner.isOnline()) {
            participation.leave(winner.getUniqueId());
            Location winnerReturn = session.returnFor(winner.getUniqueId());
            winner.teleport(winnerReturn);
            prepare(winner);
            winner.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "DUEL GEWONNEN");
            winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 0.7F, 1.5F);
        }
        loser.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "DUEL VERLOREN");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        Location back = pendingRespawns.remove(player.getUniqueId());
        if (back == null) return;
        event.setRespawnLocation(back);
        Bukkit.getScheduler().runTask(plugin, () -> {
            participation.leave(player.getUniqueId());
            prepare(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        requests.remove(quitter.getUniqueId());
        removeRequestsFrom(quitter.getUniqueId());
        Session session = sessions.get(quitter.getUniqueId());
        if (session == null || session.finished) {
            participation.leave(quitter.getUniqueId());
            return;
        }
        session.finished = true;
        sessions.remove(session.first);
        sessions.remove(session.second);
        pendingRespawns.remove(quitter.getUniqueId());
        participation.leave(quitter.getUniqueId());

        UUID opponentId = session.opponent(quitter.getUniqueId());
        Player opponent = Bukkit.getPlayer(opponentId);
        participation.leave(opponentId);
        if (opponent != null && opponent.isOnline()) {
            opponent.teleport(session.returnFor(opponentId));
            prepare(opponent);
            opponent.sendMessage(ChatColor.GREEN + "Duel gewonnen: Gegner hat den Server verlassen.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isDuel(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (isDuel(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (!isDuel(player.getUniqueId())) return;
        if (event.getInventory().getHolder() instanceof Player) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isDuel(event.getPlayer().getUniqueId())) return;
        if (event.getPlayer().hasPermission("skykings.admin.event.bypass")) return;
        String lower = event.getMessage().toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("/duel") || lower.startsWith("/duel ")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Commands sind waehrend eines Duels deaktiviert.");
    }

    public void shutdown() {
        for (Session session : new java.util.HashSet<Session>(sessions.values())) {
            if (session.finished) continue;
            session.finished = true;
            restoreIfOnline(session.first, session.firstReturn);
            restoreIfOnline(session.second, session.secondReturn);
        }
        sessions.clear();
        requests.clear();
        pendingRespawns.clear();
        participation.clear();
    }

    private void restoreIfOnline(UUID uuid, Location location) {
        participation.leave(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && location != null) player.teleport(location);
    }

    private boolean isDuel(UUID uuid) {
        EventParticipationService.Participation state = participation.get(uuid);
        return state != null && state.getType() == EventParticipationService.Type.DUEL;
    }

    private Request validRequest(UUID target) {
        Request request = requests.get(target);
        if (request == null) return null;
        if (request.expiresAt < System.currentTimeMillis()) {
            requests.remove(target);
            return null;
        }
        return request;
    }

    private void removeRequestsFrom(UUID requester) {
        java.util.Iterator<Map.Entry<UUID, Request>> iterator = requests.entrySet().iterator();
        while (iterator.hasNext()) if (requester.equals(iterator.next().getValue().requester)) iterator.remove();
    }
}
