package net.skykings.combat.collection;

import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Kleiner Bonus, wenn ein Spieler genau den Spieler besiegt, der ihn zuletzt getoetet hat. */
public final class RevengeService implements Listener {
    private static final long BASE_COIN_REWARD = 25_000L;

    private final EconomyService economy;
    private final Map<UUID, UUID> lastKiller = new ConcurrentHashMap<UUID, UUID>();

    public RevengeService(EconomyService economy) {
        this.economy = economy;
    }

    @EventHandler
    public void onKill(SkyKingsPlayerKillEvent event) {
        UUID killer = event.getKillerUuid();
        UUID victim = event.getVictimUuid();

        UUID revengeTarget = lastKiller.get(killer);
        boolean revenge = victim.equals(revengeTarget) && event.getAntiFarmMultiplier() > 0D;
        if (revenge) {
            long reward = Math.max(1L, Math.round(BASE_COIN_REWARD * event.getAntiFarmMultiplier()));
            economy.deposit(killer, reward, "REVENGE", "Revenge Kill gegen " + victim);
            lastKiller.remove(killer);

            Player player = Bukkit.getPlayer(killer);
            if (player != null) {
                player.sendMessage(UiTheme.PRIMARY + "Revenge Kill");
                player.sendMessage(UiTheme.TEXT + "+" + UiFormat.coins(reward));
                SoundFeedback.notify(player);
            }
        }

        // Der neue Tote merkt sich seinen aktuellen Killer als naechstes Revenge-Ziel.
        lastKiller.put(victim, killer);
    }

    public UUID getRevengeTarget(UUID player) {
        return lastKiller.get(player);
    }
}
