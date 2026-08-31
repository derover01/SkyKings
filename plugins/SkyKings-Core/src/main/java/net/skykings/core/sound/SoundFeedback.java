package net.skykings.core.sound;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Zentrale 1.8-kompatible Sound-Rueckmeldungen fuer SkyKings-GUIs und Interaktionen. */
public final class SoundFeedback {

    private SoundFeedback() {}

    public static void menuOpen(Player player) {
        play(player, Sound.CHEST_OPEN, 0.55F, 1.25F);
    }

    public static void click(Player player) {
        play(player, Sound.CLICK, 0.45F, 1.35F);
    }

    public static void success(Player player) {
        play(player, Sound.ORB_PICKUP, 0.7F, 1.45F);
    }

    public static void reward(Player player) {
        play(player, Sound.LEVEL_UP, 0.55F, 1.6F);
    }

    public static void confirm(Player player) {
        play(player, Sound.NOTE_PLING, 0.65F, 1.55F);
    }

    public static void error(Player player) {
        play(player, Sound.NOTE_BASS, 0.6F, 0.7F);
    }

    private static void play(Player player, Sound sound, float volume, float pitch) {
        if (player == null || !player.isOnline()) return;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
