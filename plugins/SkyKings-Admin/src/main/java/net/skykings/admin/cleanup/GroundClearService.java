package net.skykings.admin.cleanup;

import net.skykings.admin.message.SkyKingsAnnouncement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Entfernt ausschliesslich gedroppte Item-Entities. Automatisch alle 30 Minuten mit 2-Minuten-Warnung
 * und manuell ueber denselben Countdown.
 */
public final class GroundClearService {

    private static final long TICKS_PER_SECOND = 20L;
    private static final long CYCLE_TICKS = 30L * 60L * TICKS_PER_SECOND;
    private static final long COUNTDOWN_TICKS = 2L * 60L * TICKS_PER_SECOND;

    private final JavaPlugin plugin;
    private BukkitTask automaticTask;
    private BukkitTask countdownTask;
    private int secondsRemaining;

    public GroundClearService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startAutomaticCycle() {
        stopAutomaticCycle();
        long firstWarningDelay = CYCLE_TICKS - COUNTDOWN_TICKS;
        automaticTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                startCountdown(false);
            }
        }, firstWarningDelay, CYCLE_TICKS);
    }

    public void stopAutomaticCycle() {
        if (automaticTask != null) {
            automaticTask.cancel();
            automaticTask = null;
        }
        cancelCountdown();
    }

    public boolean startManualCountdown() {
        return startCountdown(true);
    }

    private boolean startCountdown(boolean manual) {
        if (countdownTask != null) return false;
        secondsRemaining = 120;
        announceCountdown(secondsRemaining, manual);
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                secondsRemaining--;
                if (secondsRemaining <= 0) {
                    int removed = clearGroundItems();
                    SkyKingsAnnouncement.broadcast("System",
                            ChatColor.GRAY + "Boden-Clear abgeschlossen. " + ChatColor.AQUA + removed
                                    + ChatColor.GRAY + " gedroppte Items wurden entfernt.");
                    cancelCountdown();
                    return;
                }
                if (shouldAnnounce(secondsRemaining)) {
                    announceCountdown(secondsRemaining, manual);
                }
            }
        }, TICKS_PER_SECOND, TICKS_PER_SECOND);
        return true;
    }

    private boolean shouldAnnounce(int seconds) {
        return seconds == 60 || seconds == 30 || seconds == 10 || seconds == 5
                || seconds == 4 || seconds == 3 || seconds == 2 || seconds == 1;
    }

    private void announceCountdown(int seconds, boolean manual) {
        String time;
        if (seconds == 120) time = "2 Minuten";
        else if (seconds == 60) time = "1 Minute";
        else time = seconds + (seconds == 1 ? " Sekunde" : " Sekunden");

        String prefix = manual ? "Ein manueller Boden-Clear" : "Der naechste Boden-Clear";
        SkyKingsAnnouncement.broadcast("System",
                ChatColor.GRAY + prefix + " startet in " + ChatColor.YELLOW + time + ChatColor.GRAY
                        + ". Hebe wichtige Items rechtzeitig auf.");
    }

    private int clearGroundItems() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Item)) continue;
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            BukkitTask task = countdownTask;
            countdownTask = null;
            task.cancel();
        }
        secondsRemaining = 0;
    }
}
