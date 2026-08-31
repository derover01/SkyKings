package net.skykings.combat.event;

import net.skykings.combat.map.zone.PvpRegionService;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.item.SkyKingsCurrencyItems;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/** 5-Minuten Most-Wanted Event ausschliesslich innerhalb konfigurierter PvP-Regionen. */
public final class TargetEventService implements Listener {
    private static final int DURATION_SECONDS = 5 * 60;
    private static final long HUNTER_COINS = 350_000L;
    private static final int HUNTER_STARS = 3;
    private static final long SURVIVOR_COINS = 250_000L;
    private static final int SURVIVOR_STARS = 2;

    private final JavaPlugin plugin;
    private final PvpRegionService regions;
    private final EconomyService economy;
    private final EventParticipationService participation;
    private final Random random = new Random();
    private UUID target;
    private int remaining;
    private BukkitTask task;
    private long lastBoundaryWarning;

    public TargetEventService(JavaPlugin plugin, PvpRegionService regions, EconomyService economy,
                              EventParticipationService participation) {
        this.plugin = plugin;
        this.regions = regions;
        this.economy = economy;
        this.participation = participation;
    }

    public boolean isActive() { return target != null; }
    public UUID getTarget() { return target; }
    public int getRemaining() { return remaining; }

    public Player startRandom() {
        if (isActive()) return null;
        List<Player> candidates = new ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!regions.isInPvpArea(player)) continue;
            if (participation != null && participation.isInEvent(player.getUniqueId())) continue;
            candidates.add(player);
        }
        if (candidates.isEmpty()) return null;
        Player chosen = candidates.get(random.nextInt(candidates.size()));
        start(chosen);
        return chosen;
    }

    public boolean start(Player chosen) {
        if (chosen == null || isActive() || !regions.isInPvpArea(chosen)
                || (participation != null && participation.isInEvent(chosen.getUniqueId()))) return false;
        target = chosen.getUniqueId();
        remaining = DURATION_SECONDS;
        broadcastStart(chosen);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        return true;
    }

    public void stop(boolean announce) {
        if (task != null) { task.cancel(); task = null; }
        UUID old = target;
        target = null;
        remaining = 0;
        if (announce && old != null) Bukkit.broadcastMessage(UiTheme.MUTED + "Most Wanted wurde beendet.");
    }

    private void tick() {
        if (target == null) { stop(false); return; }
        Player hunted = Bukkit.getPlayer(target);
        if (hunted == null || !hunted.isOnline()) { stop(false); return; }
        remaining--;
        if (remaining <= 0) {
            rewardSurvivor(hunted);
            stop(false);
            return;
        }
        if (remaining == 180 || remaining == 60 || remaining == 30 || remaining == 10) {
            Bukkit.broadcastMessage(UiTheme.WARNING + "Most Wanted " + UiTheme.TEXT + hunted.getName()
                    + UiTheme.MUTED + " • " + UiFormat.durationSeconds(remaining));
            SoundFeedback.warning(hunted);
        }
    }

    @EventHandler
    public void onKill(SkyKingsPlayerKillEvent event) {
        if (target == null || !target.equals(event.getVictimUuid())) return;
        Player killer = Bukkit.getPlayer(event.getKillerUuid());
        Player victim = Bukkit.getPlayer(event.getVictimUuid());
        if (killer == null || !regions.isInPvpArea(killer)) {
            stop(false);
            return;
        }
        long coins = event.getAntiFarmMultiplier() <= 0D ? 0L
                : Math.max(1L, Math.round(HUNTER_COINS * event.getAntiFarmMultiplier()));
        if (coins > 0L) {
            economy.deposit(killer.getUniqueId(), coins, "TARGET_EVENT", "Most Wanted Kill");
            SkyKingsCurrencyItems.give(killer, HUNTER_STARS);
        }
        Bukkit.broadcastMessage(UiTheme.PRIMARY + "Most Wanted claimed");
        Bukkit.broadcastMessage(UiTheme.TEXT + killer.getName() + UiTheme.MUTED + " besiegte "
                + UiTheme.TEXT + (victim == null ? "das Target" : victim.getName())
                + (coins > 0 ? UiTheme.MUTED + " • +" + UiFormat.coins(coins) + " • +" + HUNTER_STARS + " Sterne" : ""));
        SoundFeedback.reward(killer);
        stop(false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (target == null || !target.equals(event.getEntity().getUniqueId())) return;
        final UUID dying = target;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (target != null && target.equals(dying)) {
                Bukkit.broadcastMessage(UiTheme.MUTED + "Most Wanted endete ohne Hunter-Reward.");
                stop(false);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (target == null || !target.equals(event.getPlayer().getUniqueId()) || event.getTo() == null) return;
        if (regions.isInPvpArea(event.getTo())) return;
        if (!regions.isInPvpArea(event.getFrom())) return;
        event.setTo(event.getFrom());
        long now = System.currentTimeMillis();
        if (now - lastBoundaryWarning > 2000L) {
            lastBoundaryWarning = now;
            event.getPlayer().sendMessage(UiTheme.DANGER + "Most Wanted: Du kannst die PvP-Zone nicht verlassen.");
            SoundFeedback.warning(event.getPlayer());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (target == null || !target.equals(event.getPlayer().getUniqueId())) return;
        String cmd = event.getMessage().toLowerCase(Locale.ROOT).split(" ")[0];
        if (cmd.equals("/spawn") || cmd.equals("/is") || cmd.equals("/island") || cmd.equals("/plot") || cmd.equals("/p")
                || cmd.equals("/home") || cmd.equals("/warp") || cmd.equals("/tp") || cmd.equals("/tpa") || cmd.equals("/fly")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(UiTheme.DANGER + "Most Wanted: Teleport/Flucht ist waehrend des Events gesperrt.");
            SoundFeedback.error(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (target != null && target.equals(event.getPlayer().getUniqueId())) {
            Bukkit.broadcastMessage(UiTheme.MUTED + "Most Wanted beendet: Target hat den Server verlassen.");
            stop(false);
        }
    }

    private void rewardSurvivor(Player player) {
        economy.deposit(player.getUniqueId(), SURVIVOR_COINS, "TARGET_EVENT", "Most Wanted survived");
        SkyKingsCurrencyItems.give(player, SURVIVOR_STARS);
        Bukkit.broadcastMessage(UiTheme.PRIMARY + "Most Wanted survived");
        Bukkit.broadcastMessage(UiTheme.TEXT + player.getName() + UiTheme.MUTED + " ueberlebte 5 Minuten • +"
                + UiFormat.coins(SURVIVOR_COINS) + " • +" + SURVIVOR_STARS + " Sterne");
        SoundFeedback.reward(player);
    }

    private void broadcastStart(Player chosen) {
        Bukkit.broadcastMessage(UiTheme.WARNING + "Most Wanted");
        Bukkit.broadcastMessage(UiTheme.TEXT + chosen.getName() + UiTheme.MUTED
                + " ist fuer 05:00 das Target. Nur die PvP-Zone zaehlt.");
        for (Player online : Bukkit.getOnlinePlayers()) SoundFeedback.warning(online);
    }
}
