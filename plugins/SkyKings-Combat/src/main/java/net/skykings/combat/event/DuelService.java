package net.skykings.combat.event;

import net.skykings.combat.tag.CombatTagService;
import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.kit.KitDefinition;
import net.skykings.core.kit.KitRegistry;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Sicherer 1v1-Duel-Lifecycle mit optionalem Coin-Wager, Kit-Loadout und Escrow. */
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
        INVALID_WAGER,
        KIT_NOT_FOUND,
        LOADOUT_FAILED
    }

    private static final class Request {
        final UUID requester;
        final String arena;
        final long wager;
        final String kitId;
        final long expiresAt;

        Request(UUID requester, String arena, long wager, String kitId, long expiresAt) {
            this.requester = requester;
            this.arena = arena;
            this.wager = wager;
            this.kitId = kitId;
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
        final String kitId;
        final DuelInventorySnapshot firstSnapshot;
        final DuelInventorySnapshot secondSnapshot;
        boolean escrowed;
        boolean wagerSettled;
        boolean finished;

        Session(String id, UUID first, UUID second, Location firstReturn, Location secondReturn,
                long wager, String kitId, DuelInventorySnapshot firstSnapshot,
                DuelInventorySnapshot secondSnapshot) {
            this.id = id;
            this.first = first;
            this.second = second;
            this.firstReturn = firstReturn;
            this.secondReturn = secondReturn;
            this.wager = wager;
            this.kitId = kitId;
            this.firstSnapshot = firstSnapshot;
            this.secondSnapshot = secondSnapshot;
        }

        UUID opponent(UUID player) { return first.equals(player) ? second : first; }
        Location returnFor(UUID player) { return first.equals(player) ? firstReturn : secondReturn; }
        DuelInventorySnapshot snapshotFor(UUID player) { return first.equals(player) ? firstSnapshot : secondSnapshot; }
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

    /** Legacy-Aufruf ohne Einsatz und mit eigenem Inventar. */
    public boolean request(Player requester, Player target, String arena) {
        return request(requester, target, arena, 0L, null);
    }

    /** Legacy-Aufruf mit Einsatz und eigenem Inventar. */
    public boolean request(Player requester, Player target, String arena, long wager) {
        return request(requester, target, arena, wager, null);
    }

    /** Neue Challenge-Variante: beide Spieler erhalten dasselbe temporaere Rank-Kit. */
    public boolean request(Player requester, Player target, String arena, long wager, String kitId) {
        if (requester == null || target == null || requester.equals(target)) return false;
        if (wager < 0L || wager > MAX_WAGER) return false;
        if (isBusy(requester.getUniqueId()) || isBusy(target.getUniqueId())) return false;
        if (wager > 0L && !economy.has(requester.getUniqueId(), wager)) return false;

        String normalizedKit = normalizeKit(kitId);
        if (normalizedKit != null && resolveKit(normalizedKit) == null) return false;

        String selectedArena = arena == null || arena.trim().isEmpty()
                ? "duel" : arena.trim().toLowerCase(Locale.ROOT);
        requests.put(target.getUniqueId(), new Request(requester.getUniqueId(), selectedArena, wager, normalizedKit,
                System.currentTimeMillis() + REQUEST_TIMEOUT_MILLIS));

        requester.sendMessage(UiTheme.SUCCESS + "Duel-Anfrage gesendet");
        requester.sendMessage(UiTheme.MUTED + target.getName() + " • " + selectedArena
                + kitSuffix(normalizedKit)
                + (wager > 0L ? " • " + UiFormat.coins(wager) + " Einsatz" : ""));
        target.sendMessage(UiTheme.PRIMARY + "Duel-Anfrage");
        target.sendMessage(UiTheme.TEXT + requester.getName() + UiTheme.MUTED + " fordert dich heraus."
                + kitSuffix(normalizedKit)
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
            return start(requester, target, request.arena, 0L, request.kitId);
        }

        ConfirmationMenu.open(target,
                UiItems.item(Material.GOLD_INGOT,
                        UiTheme.TEXT + "Duel Wager",
                        UiTheme.MUTED + "Gegner " + UiTheme.TEXT + requester.getName(),
                        UiTheme.MUTED + "Kit " + UiTheme.PRIMARY + displayKit(request.kitId),
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
        StartResult result = start(requester, target, current.arena, current.wager, current.kitId);
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

    public KitRegistry getKitRegistry() { return kitRegistry(); }

    private StartResult start(Player first, Player second, String arena, long wager, String kitId) {
        if (wager < 0L || wager > MAX_WAGER) return StartResult.INVALID_WAGER;
        if (isBusy(first.getUniqueId()) || isBusy(second.getUniqueId())) return StartResult.PLAYER_BUSY;
        if (combatTags.isTagged(first.getUniqueId()) || combatTags.isTagged(second.getUniqueId())) return StartResult.COMBAT_TAGGED;
        Location a = arenas.get(arena, "a");
        Location b = arenas.get(arena, "b");
        if (a == null || b == null) return StartResult.ARENA_NOT_READY;

        KitDefinition kit = kitId == null ? null : resolveKit(kitId);
        if (kitId != null && kit == null) return StartResult.KIT_NOT_FOUND;
        DuelInventorySnapshot firstSnapshot = kit == null ? null : DuelInventorySnapshot.capture(first);
        DuelInventorySnapshot secondSnapshot = kit == null ? null : DuelInventorySnapshot.capture(second);

        Session session = new Session(UUID.randomUUID().toString(), first.getUniqueId(), second.getUniqueId(),
                first.getLocation().clone(), second.getLocation().clone(), wager, kitId, firstSnapshot, secondSnapshot);

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

        if (kit != null && (!applyKit(first, kit) || !applyKit(second, kit))) {
            rollbackStart(session, first, second);
            return StartResult.LOADOUT_FAILED;
        }

        prepare(first);
        prepare(second);
        first.sendMessage(UiTheme.PRIMARY + "Duel" + UiTheme.MUTED + " gegen " + UiTheme.TEXT + second.getName()
                + kitSuffix(kitId));
        second.sendMessage(UiTheme.PRIMARY + "Duel" + UiTheme.MUTED + " gegen " + UiTheme.TEXT + first.getName()
                + kitSuffix(kitId));
        if (wager > 0L) {
            String pot = UiFormat.coins(wager * 2L);
            first.sendMessage(UiTheme.MUTED + "Wager Pot " + UiTheme.WARNING + pot);
            second.sendMessage(UiTheme.MUTED + "Wager Pot " + UiTheme.WARNING + pot);
        }
        SoundFeedback.notify(first);
        SoundFeedback.notify(second);
        return StartResult.SUCCESS;
    }

    private boolean applyKit(Player player, KitDefinition kit) {
        try {
            DuelInventorySnapshot.clearEffects(player);
            PlayerInventory inventory = player.getInventory();
            inventory.clear();
            inventory.setArmorContents(new ItemStack[4]);
            player.setLevel(0);
            player.setExp(0F);
            player.setTotalExperience(0);

            for (ItemStack original : kit.createItems()) {
                if (original == null || original.getType() == Material.AIR) continue;
                ItemStack item = original.clone();
                String name = item.getType().name();
                if (name.endsWith("_HELMET")) inventory.setHelmet(item);
                else if (name.endsWith("_CHESTPLATE")) inventory.setChestplate(item);
                else if (name.endsWith("_LEGGINGS")) inventory.setLeggings(item);
                else if (name.endsWith("_BOOTS")) inventory.setBoots(item);
                else if (!inventory.addItem(item).isEmpty()) return false;
            }
            for (PotionEffect effect : kit.getPotionEffects()) player.addPotionEffect(effect, true);
            inventory.setHeldItemSlot(0);
            player.updateInventory();
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Duel-Kit konnte nicht angewendet werden: " + kit.getId() + " - " + ex.getMessage());
            return false;
        }
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
        restoreSnapshot(session, first);
        restoreSnapshot(session, second);
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

        UUID loserId = loser.getUniqueId();
        pendingRespawns.put(loserId, session.returnFor(loserId));
        // Entscheidend: Originalinventar noch im DeathEvent wiederherstellen. Durch keepInventory
        // wird es in den Respawn uebernommen; Quit auf dem Death-Screen kann keinen Kit-Zustand festhalten.
        restoreSnapshot(session, loser);

        UUID winnerId = session.opponent(loserId);
        settleWinner(session, winnerId);
        if (winner != null && winner.isOnline()) {
            participation.leave(winner.getUniqueId());
            restoreSnapshot(session, winner);
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
        restoreSnapshot(session, quitter);
        Location quitterReturn = session.returnFor(quitter.getUniqueId());
        if (quitterReturn != null) quitter.teleport(quitterReturn);

        UUID opponentId = session.opponent(quitter.getUniqueId());
        settleWinner(session, opponentId);
        Player opponent = Bukkit.getPlayer(opponentId);
        participation.leave(opponentId);
        if (opponent != null && opponent.isOnline()) {
            restoreSnapshot(session, opponent);
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
        String lower = event.getMessage().toLowerCase(Locale.ROOT);
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
            restoreIfOnline(session, session.first, session.firstReturn);
            restoreIfOnline(session, session.second, session.secondReturn);
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

    private void restoreIfOnline(Session session, UUID uuid, Location location) {
        participation.leave(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            restoreSnapshot(session, player);
            if (location != null) player.teleport(location);
        }
    }

    private void restoreSnapshot(Session session, Player player) {
        DuelInventorySnapshot snapshot = session.snapshotFor(player.getUniqueId());
        if (snapshot != null) snapshot.restore(player);
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

    private KitRegistry kitRegistry() {
        SkyKingsCoreAPI core = Bukkit.getServicesManager().load(SkyKingsCoreAPI.class);
        return core == null ? null : core.getKitRegistry();
    }

    private KitDefinition resolveKit(String kitId) {
        KitRegistry registry = kitRegistry();
        if (registry == null || kitId == null) return null;
        Optional<KitDefinition> kit = registry.get(kitId);
        return kit.isPresent() ? kit.get() : null;
    }

    private String normalizeKit(String raw) {
        if (raw == null || raw.trim().isEmpty() || "own".equalsIgnoreCase(raw) || "eigen".equalsIgnoreCase(raw)) return null;
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String displayKit(String kitId) {
        if (kitId == null) return "Eigenes Gear";
        KitDefinition kit = resolveKit(kitId);
        return kit == null ? kitId : kit.getDisplayName();
    }

    private String kitSuffix(String kitId) {
        return " • Kit " + displayKit(kitId);
    }

    private void sendStartError(Player player, StartResult result) {
        switch (result) {
            case ARENA_NOT_READY:
                player.sendMessage(UiTheme.DANGER + "Duel-Arena ist noch nicht eingerichtet.");
                player.sendMessage(UiTheme.MUTED + "Staff: /eventarena set duel a|b");
                break;
            case COMBAT_TAGGED:
                player.sendMessage(UiTheme.DANGER + "Einer von euch ist noch im normalen Combat.");
                break;
            case TELEPORT_FAILED:
                player.sendMessage(UiTheme.DANGER + "Duel-Teleport fehlgeschlagen.");
                break;
            case NOT_ENOUGH_MONEY:
                player.sendMessage(UiTheme.DANGER + "Einer von euch hat nicht mehr genug Coins fuer den Einsatz.");
                break;
            case INVALID_WAGER:
                player.sendMessage(UiTheme.DANGER + "Ungueltiger Duel-Einsatz.");
                break;
            case KIT_NOT_FOUND:
                player.sendMessage(UiTheme.DANGER + "Das gewaehlte Duel-Kit existiert nicht mehr.");
                break;
            case LOADOUT_FAILED:
                player.sendMessage(UiTheme.DANGER + "Duel-Kit konnte nicht sicher geladen werden. Inventare wurden wiederhergestellt.");
                break;
            case PLAYER_BUSY:
            default:
                player.sendMessage(UiTheme.DANGER + "Keine gueltige Anfrage oder Spieler bereits beschaeftigt.");
                break;
        }
        SoundFeedback.error(player);
    }
}
