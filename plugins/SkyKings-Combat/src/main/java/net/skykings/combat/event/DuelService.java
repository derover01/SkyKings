package net.skykings.combat.event;

import net.skykings.combat.tag.CombatTagService;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.ConfirmationMenu;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Sicherer 1v1-Duel-Lifecycle mit optionalem Coin-Wager und Escrow. */
public final class DuelService implements Listener {

    private static final long REQUEST_TIMEOUT_MILLIS = 30_000L;
    public static final long MAX_WAGER = 500_000_000L;

    public enum StartResult {
        SUCCESS,
        ARENA_NOT_READY,
        PLAYER_BUSY,
        COMBAT_TAGGED,
        TELEPORT_FAILED,
        NOT_ENOUGH_MONEY,
        INVALID_WAGER
    }

    private static final class Request {
        final UUID requester;
        final String arena;
        final long wager;
        final long expiresAt;
        Request(UUID requester, String arena, long wager, long expiresAt) {
            this.requester = requester;
            this.arena = arena;
            this.wager = wager;
            this.expiresAt = expiresAt;
        }
    }

    private static final class Session {
        final String id;
        final UUID first;
        final UUID second;
        final Location firstReturn;
        final Location secondReturn;
        final long wager;
        boolean escrowed;
        boolean wagerSettled;
        boolean finished;

        Session(String id, UUID first, UUID second, Location firstReturn, Location secondReturn, long wager) {
            this.id = id;
            this.first = first;
            this.second = second;
            this.firstReturn = firstReturn;
            this.secondReturn = secondReturn;
            this.wager = wager;
        }

        UUID opponent(UUID player) { return first.equals(player) ? second : first; }
        Location returnFor(UUID player) { return first.equals(player) ? firstReturn : secondReturn; }
    }

    private final JavaPlugin plugin;
    private final EventArenaService arenas;
    private final CombatTagService combatTags;
    private final EconomyService economy;
    private final EventParticipationService participation = EventParticipationService.global();
    private final Map<UUID, Request> requests = new HashMap<UUID, Request>();
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final Map<UUID, Location> pendingRespawns = new HashMap<UUID, Location>();

    public DuelService(JavaPlugin plugin, EventArenaService arenas, CombatTagService combatTags, EconomyService economy) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.combatTags = combatTags;
        this.economy = economy;
    }

    /** Legacy-Aufruf ohne Einsatz. */
    public boolean request(Player requester, Player target, String arena) {
        return request(requester, target, arena, 0L);
    }

    public boolean request(Player requester, Player target, String arena, long wager) {
        if (requester == null || target == null || requester.equals(target)) return false;
        if (wager < 0L || wager > MAX_WAGER) return false;
        if (isBusy(requester.getUniqueId()) || isBusy(target.getUniqueId())) return false;
        if (wager > 0L && !economy.has(requester.getUniqueId(), wager)) return false;

        String selectedArena = arena == null || arena.trim().isEmpty()
                ? "duel" : arena.trim().toLowerCase(java.util.Locale.ROOT);
        requests.put(target.getUniqueId(), new Request(requester.getUniqueId(), selectedArena, wager,
                System.currentTimeMillis() + REQUEST_TIMEOUT_MILLIS));

        requester.sendMessage(UiTheme.SUCCESS + "Duel-Anfrage gesendet");
        requester.sendMessage(UiTheme.MUTED + target.getName() + " • " + selectedArena
                + (wager > 0L ? " • " + UiFormat.coins(wager) + " Einsatz" : ""));
        target.sendMessage(UiTheme.PRIMARY + "Duel-Anfrage");
        target.sendMessage(UiTheme.TEXT + requester.getName() + UiTheme.MUTED + " fordert dich heraus."
                + (wager > 0L ? " Einsatz: " + UiTheme.WARNING + UiFormat.coins(wager) : ""));
        target.sendMessage(UiTheme.WARNING + "/duel accept" + UiTheme.MUTED + " oder " + UiTheme.WARNING + "/duel deny");
        SoundFeedback.notify(target);
        return true;
    }

    /** Oeffnet bei Wager zuerst die verbindliche Confirmation; 0-Coin-Duels starten sofort. */
    public StartResult accept(Player target) {
        final Request request = validRequest(target.getUniqueId());
        if (request == null) return StartResult.PLAYER_BUSY;
        final Player requester = Bukkit.getPlayer(request.requester);
        if (requester == null || !requester.isOnline()) {
            requests.remove(target.getUniqueId());
            return StartResult.PLAYER_BUSY;
        }

        if (request.wager <= 0L) {
            requests.remove(target.getUniqueId());
            return start(requester, target, request.arena, 0L);
        }

        ConfirmationMenu.open(target,
                UiItems.item(Material.GOLD_INGOT,
                        UiTheme.TEXT + "Duel Wager",
                        UiTheme.MUTED + "Gegner " + UiTheme.TEXT + requester.getName(),
                        UiTheme.MUTED + "Dein Einsatz " + UiTheme.WARNING + UiFormat.coins(request.wager),
                        UiTheme.MUTED + "Gewinner-Pot " + UiTheme.TEXT + UiFormat.coins(request.wager * 2L)),
                "Duel Wager",
                UiFormat.coins(request.wager) + " einsetzen",
                () -> acceptConfirmed(target, request),
                null);
        return StartResult.SUCCESS;
    }

    private void acceptConfirmed(Player target, Request expected) {
        Request current = validRequest(target.getUniqueId());
        if (current == null || current != expected) {
            target.sendMessage(UiTheme.DANGER + "Duel-Anfrage ist abgelaufen.");
            SoundFeedback.error(target);
            return;
        }
        requests.remove(target.getUniqueId());
        Player requester = Bukkit.getPlayer(current.requester);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(UiTheme.DANGER + "Herausforderer ist nicht mehr online.");
            SoundFeedback.error(target);
            return;
        }
        StartResult result = start(requester, target, current.arena, current.wager);
        if (result != StartResult.SUCCESS) sendStartError(target, result);
    }

    public boolean deny(Player target) {
        Request request = validRequest(target.getUniqueId());
        if (request == null) return false;
        requests.remove(target.getUniqueId());
        Player requester = Bukkit.getPlayer(request.requester);
        if (requester != null) {
            requester.sendMessage(UiTheme.MUTED + target.getName() + " hat die Duel-Anfrage abgelehnt.");
            SoundFeedback.back(requester);
        }
        target.sendMessage(UiTheme.MUTED + "Duel-Anfrage abgelehnt.");
        SoundFeedback.back(target);
        return true;
    }

    public boolean hasPendingRequest(UUID target) { return validRequest(target) != null; }
    public boolean isBusy(UUID uuid) { return participation.isInEvent(uuid) || sessions.containsKey(uuid); }

    private StartResult start(Player first, Player second, String arena, long wager) {
        if (wager < 0L || wager > MAX_WAGER) return StartResult.INVALID_WAGER;
        if (isBusy(first.getUniqueId()) || isBusy(second.getUniqueId())) return StartResult.PLAYER_BUSY;
        if (combatTags.isTagged(first.getUniqueId()) || combatTags.isTagged(second.getUniqueId())) return StartResult.COMBAT_TAGGED;
        Location a = arenas.get(arena, "a");
        Location b = arenas.get(arena, "b");
        if (a == null || b == null) return StartResult.ARENA_NOT_READY;

        Session session = new Session(UUID.randomUUID().toString(), first.getUniqueId(), second.getUniqueId(),
                first.getLocation().clone(), second.getLocation().clone(), wager);

        if (wager > 0L && !takeEscrow(session, first, second)) return StartResult.NOT_ENOUGH_MONEY;

        if (!participation.join(first.getUniqueId(), EventParticipationService.Type.DUEL, session.id)) {
            refundEscrow(session);
            return StartResult.PLAYER_BUSY;
        }
        if (!participation.join(second.getUniqueId(), EventParticipationService.Type.DUEL, session.id)) {
            participation.leave(first.getUniqueId());
            refundEscrow(session);
            return StartResult.PLAYER_BUSY;
        }

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
        first.sendMessage(UiTheme.PRIMARY + "Duel" + UiTheme.MUTED + " gegen " + UiTheme.TEXT + second.getName());
        second.sendMessage(UiTheme.PRIMARY + "Duel" + UiTheme.MUTED + " gegen " + UiTheme.TEXT + first.getName());
        if (wager > 0L) {
            String pot = UiFormat.coins(wager * 2L);
            first.sendMessage(UiTheme.MUTED + "Wager Pot " + UiTheme.WARNING + pot);
            second.sendMessage(UiTheme.MUTED + "Wager Pot " + UiTheme.WARNING + pot);
        }
        SoundFeedback.notify(first);
        SoundFeedback.notify(second);
        return StartResult.SUCCESS;
    }

    private boolean takeEscrow(Session session, Player first, Player second) {
        if (!economy.withdraw(first.getUniqueId(), session.wager, "DUEL_WAGER", "Escrow gegen " + second.getName())) return false;
        if (!economy.withdraw(second.getUniqueId(), session.wager, "DUEL_WAGER", "Escrow gegen " + first.getName())) {
            economy.deposit(first.getUniqueId(), session.wager, "DUEL_WAGER_REFUND", "Gegner konnte Einsatz nicht hinterlegen");
            return false;
        }
        session.escrowed = true;
        return true;
    }

    private void rollbackStart(Session session, Player first, Player second) {
        sessions.remove(session.first);
        sessions.remove(session.second);
        participation.leave(session.first);
        participation.leave(session.second);
        refundEscrow(session);
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

        UUID winnerId = session.opponent(loser.getUniqueId());
        settleWinner(session, winnerId);
        if (winner != null && winner.isOnline()) {
            participation.leave(winner.getUniqueId());
            Location winnerReturn = session.returnFor(winner.getUniqueId());
            winner.teleport(winnerReturn);
            prepare(winner);
            winner.sendMessage(UiTheme.SUCCESS + "Duel gewonnen");
            if (session.wager > 0L) winner.sendMessage(UiTheme.TEXT + "+" + UiFormat.coins(session.wager * 2L));
            SoundFeedback.reward(winner);
        }
        loser.sendMessage(UiTheme.DANGER + "Duel verloren");
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
        settleWinner(session, opponentId);
        Player opponent = Bukkit.getPlayer(opponentId);
        participation.leave(opponentId);
        if (opponent != null && opponent.isOnline()) {
            opponent.teleport(session.returnFor(opponentId));
            prepare(opponent);
            opponent.sendMessage(UiTheme.SUCCESS + "Duel gewonnen" + UiTheme.MUTED + " • Gegner hat aufgegeben.");
            if (session.wager > 0L) opponent.sendMessage(UiTheme.TEXT + "+" + UiFormat.coins(session.wager * 2L));
            SoundFeedback.reward(opponent);
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
        event.getPlayer().sendMessage(UiTheme.DANGER + "Commands sind waehrend eines Duels deaktiviert.");
        SoundFeedback.error(event.getPlayer());
    }

    public void shutdown() {
        for (Session session : new HashSet<Session>(sessions.values())) {
            if (session.finished) continue;
            session.finished = true;
            refundEscrow(session);
            restoreIfOnline(session.first, session.firstReturn);
            restoreIfOnline(session.second, session.secondReturn);
        }
        sessions.clear();
        requests.clear();
        pendingRespawns.clear();
        participation.clear();
    }

    private void settleWinner(Session session, UUID winner) {
        if (!session.escrowed || session.wagerSettled || session.wager <= 0L) return;
        session.wagerSettled = true;
        session.escrowed = false;
        economy.deposit(winner, session.wager * 2L, "DUEL_WAGER_WIN", "Duel Pot " + session.id);
    }

    private void refundEscrow(Session session) {
        if (!session.escrowed || session.wagerSettled || session.wager <= 0L) return;
        session.wagerSettled = true;
        session.escrowed = false;
        economy.deposit(session.first, session.wager, "DUEL_WAGER_REFUND", "Duel nicht abgeschlossen " + session.id);
        economy.deposit(session.second, session.wager, "DUEL_WAGER_REFUND", "Duel nicht abgeschlossen " + session.id);
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

    private void sendStartError(Player player, StartResult result) {
        switch (result) {
            case ARENA_NOT_READY: player.sendMessage(UiTheme.DANGER + "Duel-Arena ist nicht eingerichtet."); break;
            case COMBAT_TAGGED: player.sendMessage(UiTheme.DANGER + "Einer von euch ist noch im normalen Combat."); break;
            case TELEPORT_FAILED: player.sendMessage(UiTheme.DANGER + "Duel-Teleport fehlgeschlagen. Einsatz wurde erstattet."); break;
            case NOT_ENOUGH_MONEY: player.sendMessage(UiTheme.DANGER + "Einer von euch hat nicht mehr genug Coins fuer den Einsatz."); break;
            case INVALID_WAGER: player.sendMessage(UiTheme.DANGER + "Ungueltiger Duel-Einsatz."); break;
            case PLAYER_BUSY:
            default: player.sendMessage(UiTheme.DANGER + "Duel-Anfrage ist nicht mehr gueltig."); break;
        }
        SoundFeedback.error(player);
    }
}
