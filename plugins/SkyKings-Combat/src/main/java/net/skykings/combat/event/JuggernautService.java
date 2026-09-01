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
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Ein zufaelliger Juggernaut gegen alle anderen Teilnehmer. */
public final class JuggernautService implements Listener {
    public static final long BOSS_WIN_REWARD = 1_000_000L;
    public static final long ATTACKER_WIN_REWARD = 250_000L;
    private static final double BOSS_MAX_HEALTH = 40D;

    private static final class PlayerState {
        final Location returnLocation;
        final double maxHealth;
        PlayerState(Location returnLocation, double maxHealth) {
            this.returnLocation = returnLocation;
            this.maxHealth = maxHealth;
        }
    }

    private final JavaPlugin plugin;
    private final EventArenaService arenas;
    private final EconomyService economy;
    private final EventParticipationService participation = EventParticipationService.global();
    private final Set<UUID> queue = new LinkedHashSet<UUID>();
    private final Set<UUID> aliveAttackers = new LinkedHashSet<UUID>();
    private final Map<UUID, PlayerState> states = new LinkedHashMap<UUID, PlayerState>();
    private final Map<UUID, Location> pendingRespawns = new LinkedHashMap<UUID, Location>();

    private boolean running;
    private String sessionId;
    private UUID bossId;

    public JuggernautService(JavaPlugin plugin, EventArenaService arenas, EconomyService economy) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.economy = economy;
    }

    public boolean join(Player player) {
        if (player == null || running || participation.isInEvent(player.getUniqueId())) return false;
        boolean added = queue.add(player.getUniqueId());
        if (added) {
            player.sendMessage(UiTheme.SUCCESS + "Juggernaut Queue beigetreten" + UiTheme.MUTED + " • " + queue.size() + " Spieler");
            SoundFeedback.success(player);
        }
        return added;
    }

    public boolean leave(Player player) {
        if (player == null || running) return false;
        boolean removed = queue.remove(player.getUniqueId());
        if (removed) player.sendMessage(UiTheme.MUTED + "Juggernaut Queue verlassen.");
        return removed;
    }

    public int queueSize() { return queue.size(); }
    public boolean isRunning() { return running; }
    public UUID getBossId() { return bossId; }
    public int attackersAlive() { return aliveAttackers.size(); }

    public boolean start() {
        if (running || queue.size() < 3 || !arenaReady()) return false;

        List<UUID> candidates = new ArrayList<UUID>(queue);
        queue.clear();
        Collections.shuffle(candidates);
        sessionId = "juggernaut-" + System.currentTimeMillis();
        running = true;

        List<UUID> admitted = new ArrayList<UUID>();
        for (UUID uuid : candidates) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if (!participation.join(uuid, EventParticipationService.Type.JUGGERNAUT, sessionId)) continue;
            states.put(uuid, new PlayerState(player.getLocation().clone(), player.getMaxHealth()));
            admitted.add(uuid);
        }
        if (admitted.size() < 3) {
            for (UUID uuid : admitted) restore(uuid);
            resetRuntime();
            return false;
        }

        bossId = admitted.get(0);
        for (int i = 1; i < admitted.size(); i++) aliveAttackers.add(admitted.get(i));
        Player boss = Bukkit.getPlayer(bossId);
        Location bossSpawn = arenas.get("juggernaut", "boss");
        if (boss == null || bossSpawn == null) {
            stop(false);
            return false;
        }
        prepareBoss(boss);
        boss.teleport(bossSpawn);

        int spawnCount = Math.max(1, arenas.countPrefix("juggernaut", "spawn"));
        int cursor = 1;
        for (UUID uuid : aliveAttackers) {
            Player attacker = Bukkit.getPlayer(uuid);
            if (attacker == null || !attacker.isOnline()) continue;
            prepareAttacker(attacker);
            Location spawn = arenas.get("juggernaut", "spawn" + cursor);
            if (spawn == null) spawn = arenas.get("juggernaut", "lobby");
            if (spawn != null) attacker.teleport(spawn);
            cursor++;
            if (cursor > spawnCount) cursor = 1;
        }

        broadcast(UiTheme.LEGENDARY + "JUGGERNAUT" + UiTheme.MUTED + " • " + UiTheme.TEXT + boss.getName()
                + UiTheme.MUTED + " gegen " + UiTheme.TEXT + aliveAttackers.size() + UiTheme.MUTED + " Angreifer.");
        boss.sendMessage(UiTheme.LEGENDARY + "DU BIST DER JUGGERNAUT" + UiTheme.MUTED + " • 40 HP + Staerke + Resistenz");
        SoundFeedback.notify(boss);
        return true;
    }

    public void stop(boolean announce) {
        if (!running && queue.isEmpty()) return;
        if (announce) broadcast(UiTheme.DANGER + "Juggernaut wurde von Staff beendet.");
        for (UUID uuid : new ArrayList<UUID>(states.keySet())) restore(uuid);
        queue.clear();
        resetRuntime();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        if (!running) return;
        UUID loser = event.getEntity().getUniqueId();
        if (!isParticipant(loser)) return;

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
        event.setDeathMessage(null);

        PlayerState state = states.get(loser);
        if (state != null) pendingRespawns.put(loser, state.returnLocation);
        participation.leave(loser);

        if (loser.equals(bossId)) {
            event.getEntity().sendMessage(UiTheme.DANGER + "Der Juggernaut wurde besiegt.");
            Bukkit.getScheduler().runTaskLater(plugin, this::attackersWin, 2L);
            return;
        }

        aliveAttackers.remove(loser);
        event.getEntity().sendMessage(UiTheme.DANGER + "Aus Juggernaut ausgeschieden.");
        if (aliveAttackers.isEmpty()) Bukkit.getScheduler().runTaskLater(plugin, this::bossWins, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Location back = pendingRespawns.remove(uuid);
        if (back == null) return;
        event.setRespawnLocation(back);
        Bukkit.getScheduler().runTask(plugin, () -> restoreHealthOnly(event.getPlayer(), uuid));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        queue.remove(uuid);
        if (!running || !isParticipant(uuid)) return;
        participation.leave(uuid);
        if (uuid.equals(bossId)) {
            states.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, this::attackersWin);
            return;
        }
        aliveAttackers.remove(uuid);
        states.remove(uuid);
        if (aliveAttackers.isEmpty()) Bukkit.getScheduler().runTask(plugin, this::bossWins);
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
        if (lower.equals("/juggernaut") || lower.startsWith("/juggernaut ")
                || lower.equals("/jug") || lower.startsWith("/jug ")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(UiTheme.DANGER + "Commands sind waehrend Juggernaut deaktiviert.");
        SoundFeedback.error(event.getPlayer());
    }

    private void attackersWin() {
        if (!running) return;
        int rewarded = 0;
        for (UUID uuid : new ArrayList<UUID>(aliveAttackers)) {
            if (!isParticipant(uuid)) continue;
            economy.deposit(uuid, ATTACKER_WIN_REWARD, "JUGGERNAUT_WIN", "Juggernaut besiegt");
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(UiTheme.SUCCESS + "JUGGERNAUT BESIEGT" + UiTheme.MUTED + " • +250.000 Coins");
                SoundFeedback.reward(player);
            }
            rewarded++;
        }
        broadcast(UiTheme.SUCCESS + "JUGGERNAUT BESIEGT" + UiTheme.MUTED + " • " + rewarded + " Angreifer ueberleben.");
        finish();
    }

    private void bossWins() {
        if (!running) return;
        Player boss = bossId == null ? null : Bukkit.getPlayer(bossId);
        if (bossId != null && isParticipant(bossId)) {
            economy.deposit(bossId, BOSS_WIN_REWARD, "JUGGERNAUT_BOSS_WIN", "Alle Angreifer besiegt");
            if (boss != null) {
                boss.sendMessage(UiTheme.LEGENDARY + "JUGGERNAUT VICTORY" + UiTheme.MUTED + " • +1.000.000 Coins");
                SoundFeedback.reward(boss);
            }
        }
        broadcast(UiTheme.LEGENDARY + "JUGGERNAUT VICTORY" + UiTheme.MUTED + " • alle Angreifer wurden besiegt.");
        finish();
    }

    private void finish() {
        for (UUID uuid : new ArrayList<UUID>(states.keySet())) restore(uuid);
        resetRuntime();
    }

    private void prepareBoss(Player player) {
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setMaxHealth(BOSS_MAX_HEALTH);
        player.setHealth(BOSS_MAX_HEALTH);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0), true);
    }

    private void prepareAttacker(Player player) {
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20F);
        PlayerState state = states.get(player.getUniqueId());
        double max = state == null ? 20D : state.maxHealth;
        player.setMaxHealth(max);
        player.setHealth(Math.min(max, 20D));
    }

    private void restore(UUID uuid) {
        participation.leave(uuid);
        PlayerState state = states.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || state == null) return;
        restoreHealthOnly(player, uuid, state);
        player.teleport(state.returnLocation);
    }

    private void restoreHealthOnly(Player player, UUID uuid) {
        PlayerState state = states.remove(uuid);
        if (state != null) restoreHealthOnly(player, uuid, state);
    }

    private void restoreHealthOnly(Player player, UUID uuid, PlayerState state) {
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        player.setMaxHealth(Math.max(1D, state.maxHealth));
        player.setHealth(Math.min(player.getMaxHealth(), 20D));
        player.setFireTicks(0);
    }

    private boolean arenaReady() {
        return arenas.get("juggernaut", "boss") != null
                && arenas.get("juggernaut", "lobby") != null
                && arenas.countPrefix("juggernaut", "spawn") >= 2;
    }

    private boolean isParticipant(UUID uuid) {
        EventParticipationService.Participation state = participation.get(uuid);
        return state != null && state.getType() == EventParticipationService.Type.JUGGERNAUT
                && sessionId != null && sessionId.equals(state.getSessionId());
    }

    private void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(message);
    }

    private void resetRuntime() {
        aliveAttackers.clear();
        states.clear();
        pendingRespawns.clear();
        running = false;
        sessionId = null;
        bossId = null;
    }

    public void shutdown() { stop(false); }
}
