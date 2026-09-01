package net.skykings.combat.event;

import net.skykings.core.clan.ClanService;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sicherer 2v2- bis 5v5-Clan-War auf Basis der echten Core-Clans. */
public final class ClanWarService implements Listener {
    public static final int MIN_TEAM_SIZE = 2;
    public static final int MAX_TEAM_SIZE = 5;
    public static final long WIN_REWARD_PER_PLAYER = 500_000L;
    private static final long CHALLENGE_MILLIS = 60_000L;

    private static final class Challenge {
        final UUID challengerOwner;
        final UUID challengerClan;
        final UUID targetClan;
        final long expiresAt;
        Challenge(UUID challengerOwner, UUID challengerClan, UUID targetClan) {
            this.challengerOwner = challengerOwner;
            this.challengerClan = challengerClan;
            this.targetClan = targetClan;
            this.expiresAt = System.currentTimeMillis() + CHALLENGE_MILLIS;
        }
    }

    private final JavaPlugin plugin;
    private final EventArenaService arenas;
    private final ClanService clans;
    private final EconomyService economy;
    private final EventParticipationService participation = EventParticipationService.global();
    private final Map<UUID, Challenge> challenges = new LinkedHashMap<UUID, Challenge>();
    private final Map<UUID, Location> returnLocations = new LinkedHashMap<UUID, Location>();
    private final Map<UUID, Location> pendingRespawns = new LinkedHashMap<UUID, Location>();
    private final Set<UUID> teamA = new LinkedHashSet<UUID>();
    private final Set<UUID> teamB = new LinkedHashSet<UUID>();

    private boolean running;
    private String sessionId;
    private UUID clanAId;
    private UUID clanBId;
    private String clanATag;
    private String clanBTag;

    public ClanWarService(JavaPlugin plugin, EventArenaService arenas, ClanService clans, EconomyService economy) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.clans = clans;
        this.economy = economy;
    }

    public boolean challenge(Player challenger, Player targetOwner) {
        if (challenger == null || targetOwner == null || running) return false;
        ClanService.Clan a = clans.getClan(challenger.getUniqueId());
        ClanService.Clan b = clans.getClan(targetOwner.getUniqueId());
        if (a == null || b == null || a.getId().equals(b.getId())) return false;
        if (!a.isOwner(challenger.getUniqueId()) || !b.isOwner(targetOwner.getUniqueId())) return false;
        if (onlineReadyMembers(a).size() < MIN_TEAM_SIZE || onlineReadyMembers(b).size() < MIN_TEAM_SIZE) return false;

        challenges.put(targetOwner.getUniqueId(), new Challenge(challenger.getUniqueId(), a.getId(), b.getId()));
        challenger.sendMessage(UiTheme.SUCCESS + "Clan-War Anfrage gesendet" + UiTheme.MUTED + " • [" + b.getTag() + "] " + b.getName());
        targetOwner.sendMessage(UiTheme.PRIMARY + "CLAN WAR" + UiTheme.MUTED + " • [" + a.getTag() + "] fordert deinen Clan heraus.");
        targetOwner.sendMessage(UiTheme.WARNING + "/clanwar accept" + UiTheme.MUTED + " oder " + UiTheme.WARNING + "/clanwar deny");
        SoundFeedback.notify(targetOwner);
        return true;
    }

    public boolean deny(Player targetOwner) {
        return targetOwner != null && challenges.remove(targetOwner.getUniqueId()) != null;
    }

    public StartResult accept(Player targetOwner) {
        if (targetOwner == null || running) return StartResult.BUSY;
        Challenge challenge = challenges.remove(targetOwner.getUniqueId());
        if (challenge == null || challenge.expiresAt < System.currentTimeMillis()) return StartResult.NO_CHALLENGE;
        ClanService.Clan targetClan = clans.getClan(targetOwner.getUniqueId());
        ClanService.Clan challengerClan = findClan(challenge.challengerClan);
        if (targetClan == null || challengerClan == null || !targetClan.isOwner(targetOwner.getUniqueId())
                || !targetClan.getId().equals(challenge.targetClan)) return StartResult.INVALID_CLAN;
        return start(challengerClan, targetClan);
    }

    public enum StartResult {
        SUCCESS, NO_CHALLENGE, INVALID_CLAN, NOT_ENOUGH_PLAYERS, ARENA_NOT_READY, BUSY
    }

    public boolean isRunning() { return running; }
    public String status() {
        if (!running) return "Kein Clan War aktiv";
        return "[" + clanATag + "] " + teamA.size() + " vs " + teamB.size() + " [" + clanBTag + "]";
    }

    private StartResult start(ClanService.Clan a, ClanService.Clan b) {
        if (running) return StartResult.BUSY;
        if (!arenaReady()) return StartResult.ARENA_NOT_READY;
        List<UUID> rosterA = onlineReadyMembers(a);
        List<UUID> rosterB = onlineReadyMembers(b);
        int teamSize = Math.min(MAX_TEAM_SIZE, Math.min(rosterA.size(), rosterB.size()));
        if (teamSize < MIN_TEAM_SIZE) return StartResult.NOT_ENOUGH_PLAYERS;
        rosterA = new ArrayList<UUID>(rosterA.subList(0, teamSize));
        rosterB = new ArrayList<UUID>(rosterB.subList(0, teamSize));

        sessionId = "clanwar-" + System.currentTimeMillis();
        clanAId = a.getId();
        clanBId = b.getId();
        clanATag = a.getTag();
        clanBTag = b.getTag();

        List<UUID> admittedA = admit(rosterA, teamA);
        List<UUID> admittedB = admit(rosterB, teamB);
        if (admittedA.size() < MIN_TEAM_SIZE || admittedB.size() < MIN_TEAM_SIZE) {
            for (UUID uuid : new ArrayList<UUID>(returnLocations.keySet())) restore(uuid);
            resetRuntime();
            return StartResult.NOT_ENOUGH_PLAYERS;
        }
        int balanced = Math.min(admittedA.size(), admittedB.size());
        trimTeam(teamA, balanced);
        trimTeam(teamB, balanced);
        running = true;

        teleportTeam(teamA, "a");
        teleportTeam(teamB, "b");
        broadcast(UiTheme.PRIMARY + "CLAN WAR" + UiTheme.MUTED + " • [" + clanATag + "] "
                + UiTheme.TEXT + teamA.size() + "v" + teamB.size() + UiTheme.MUTED + " [" + clanBTag + "]");
        return StartResult.SUCCESS;
    }

    private List<UUID> admit(List<UUID> roster, Set<UUID> team) {
        List<UUID> admitted = new ArrayList<UUID>();
        for (UUID uuid : roster) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!participation.join(uuid, EventParticipationService.Type.CLAN_WAR, sessionId)) continue;
            returnLocations.put(uuid, player.getLocation().clone());
            team.add(uuid);
            admitted.add(uuid);
        }
        return admitted;
    }

    private void trimTeam(Set<UUID> team, int size) {
        while (team.size() > size) {
            UUID last = null;
            for (UUID uuid : team) last = uuid;
            if (last == null) break;
            team.remove(last);
            restore(last);
        }
    }

    private void teleportTeam(Set<UUID> team, String prefix) {
        int i = 1;
        for (UUID uuid : team) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            Location spawn = arenas.get("clanwar", prefix + i);
            if (spawn == null) spawn = arenas.get("clanwar", prefix + "1");
            prepare(player);
            if (spawn != null) player.teleport(spawn);
            player.sendMessage(UiTheme.PRIMARY + "CLAN WAR" + UiTheme.MUTED + " • Team ["
                    + (team == teamA ? clanATag : clanBTag) + "]");
            i++;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;
        Player attacker = resolvePlayer(event.getDamager());
        boolean victimIn = victim != null && isParticipant(victim.getUniqueId());
        boolean attackerIn = attacker != null && isParticipant(attacker.getUniqueId());
        if (!victimIn && !attackerIn) return;
        if (victim == null || attacker == null) {
            event.setCancelled(true);
            return;
        }
        UUID v = victim.getUniqueId(), a = attacker.getUniqueId();
        boolean enemies = (teamA.contains(v) && teamB.contains(a)) || (teamB.contains(v) && teamA.contains(a));
        if (!enemies) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        UUID loser = event.getEntity().getUniqueId();
        if (!running || !isParticipant(loser)) return;
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
        event.setDeathMessage(null);

        Location back = returnLocations.get(loser);
        if (back != null) pendingRespawns.put(loser, back);
        participation.leave(loser);
        boolean wasA = teamA.remove(loser);
        teamB.remove(loser);
        event.getEntity().sendMessage(UiTheme.DANGER + "Aus dem Clan War ausgeschieden.");

        if (teamA.isEmpty()) Bukkit.getScheduler().runTaskLater(plugin, () -> finish(false), 2L);
        else if (teamB.isEmpty()) Bukkit.getScheduler().runTaskLater(plugin, () -> finish(true), 2L);
        else broadcast(UiTheme.MUTED + "Clan War • " + UiTheme.TEXT + teamA.size() + UiTheme.MUTED
                + " [" + clanATag + "] vs [" + clanBTag + "] " + UiTheme.TEXT + teamB.size());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Location back = pendingRespawns.remove(event.getPlayer().getUniqueId());
        if (back != null) event.setRespawnLocation(back);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        challenges.remove(uuid);
        if (!running || !isParticipant(uuid)) return;
        participation.leave(uuid);
        returnLocations.remove(uuid);
        teamA.remove(uuid);
        teamB.remove(uuid);
        if (teamA.isEmpty()) Bukkit.getScheduler().runTask(plugin, () -> finish(false));
        else if (teamB.isEmpty()) Bukkit.getScheduler().runTask(plugin, () -> finish(true));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isParticipant(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (isParticipant(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (isParticipant(player.getUniqueId()) && !(event.getInventory().getHolder() instanceof Player)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isParticipant(event.getPlayer().getUniqueId())) return;
        if (event.getPlayer().hasPermission("skykings.admin.event.bypass")) return;
        String lower = event.getMessage().toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("/clanwar") || lower.startsWith("/clanwar ") || lower.equals("/cw") || lower.startsWith("/cw ")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(UiTheme.DANGER + "Commands sind waehrend des Clan Wars deaktiviert.");
        SoundFeedback.error(event.getPlayer());
    }

    private void finish(boolean aWon) {
        if (!running) return;
        Set<UUID> winners = aWon ? new LinkedHashSet<UUID>(teamA) : new LinkedHashSet<UUID>(teamB);
        String winnerTag = aWon ? clanATag : clanBTag;
        for (UUID uuid : winners) {
            if (!isParticipant(uuid)) continue;
            economy.deposit(uuid, WIN_REWARD_PER_PLAYER, "CLAN_WAR_WIN", "Clan War [" + winnerTag + "]");
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(UiTheme.SUCCESS + "CLAN WAR GEWONNEN" + UiTheme.MUTED + " • +500.000 Coins");
                SoundFeedback.reward(player);
            }
        }
        broadcast(UiTheme.LEGENDARY + "CLAN WAR SIEGER" + UiTheme.MUTED + " • [" + winnerTag + "]");
        for (UUID uuid : new ArrayList<UUID>(returnLocations.keySet())) restore(uuid);
        resetRuntime();
    }

    public void stop(boolean announce) {
        if (!running) return;
        if (announce) broadcast(UiTheme.DANGER + "Clan War wurde von Staff beendet.");
        for (UUID uuid : new ArrayList<UUID>(returnLocations.keySet())) restore(uuid);
        resetRuntime();
    }

    private List<UUID> onlineReadyMembers(ClanService.Clan clan) {
        List<UUID> list = new ArrayList<UUID>();
        for (UUID uuid : clan.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !participation.isInEvent(uuid)) list.add(uuid);
        }
        Collections.sort(list, new Comparator<UUID>() {
            @Override public int compare(UUID x, UUID y) {
                if (x.equals(clan.getOwner())) return -1;
                if (y.equals(clan.getOwner())) return 1;
                String xn = Bukkit.getOfflinePlayer(x).getName();
                String yn = Bukkit.getOfflinePlayer(y).getName();
                if (xn == null) xn = x.toString();
                if (yn == null) yn = y.toString();
                return xn.compareToIgnoreCase(yn);
            }
        });
        return list;
    }

    private ClanService.Clan findClan(UUID clanId) {
        for (ClanService.Clan clan : clans.all()) if (clan.getId().equals(clanId)) return clan;
        return null;
    }

    private boolean arenaReady() {
        return arenas.get("clanwar", "a1") != null && arenas.get("clanwar", "a2") != null
                && arenas.get("clanwar", "b1") != null && arenas.get("clanwar", "b2") != null;
    }

    private boolean isParticipant(UUID uuid) {
        EventParticipationService.Participation state = participation.get(uuid);
        return state != null && state.getType() == EventParticipationService.Type.CLAN_WAR
                && sessionId != null && sessionId.equals(state.getSessionId());
    }

    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Projectile) {
            Object shooter = ((Projectile) entity).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    private void restore(UUID uuid) {
        participation.leave(uuid);
        Player player = Bukkit.getPlayer(uuid);
        Location back = returnLocations.remove(uuid);
        if (player != null && player.isOnline() && back != null) {
            prepare(player);
            player.teleport(back);
        }
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
        teamA.clear(); teamB.clear(); returnLocations.clear(); pendingRespawns.clear();
        running = false; sessionId = null; clanAId = null; clanBId = null; clanATag = null; clanBTag = null;
    }

    public void shutdown() { stop(false); }
}
