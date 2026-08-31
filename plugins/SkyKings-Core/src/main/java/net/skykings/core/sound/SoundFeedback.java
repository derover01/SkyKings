package net.skykings.core.sound;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Zentrale 1.8-kompatible Sound-Kategorien nach dem SkyKings UI/UX Design System. */
public final class SoundFeedback {

    private SoundFeedback() {}

    /** UI_OPEN */
    public static void menuOpen(Player player) { play(player, Sound.CHEST_OPEN, 0.45F, 1.25F); }

    /** UI_CLICK */
    public static void click(Player player) { play(player, Sound.CLICK, 0.4F, 1.35F); }

    /** UI_BACK */
    public static void back(Player player) { play(player, Sound.CLICK, 0.4F, 0.9F); }

    /** UI_SUCCESS */
    public static void success(Player player) { play(player, Sound.ORB_PICKUP, 0.65F, 1.45F); }

    /** UI_REWARD */
    public static void reward(Player player) { play(player, Sound.LEVEL_UP, 0.55F, 1.6F); }

    /** UI_LEVEL_UP */
    public static void levelUp(Player player) { play(player, Sound.LEVEL_UP, 0.7F, 1.25F); }

    /** UI_CONFIRM */
    public static void confirm(Player player) { play(player, Sound.NOTE_PLING, 0.6F, 1.55F); }

    /** UI_NOTIFY */
    public static void notify(Player player) { play(player, Sound.NOTE_PLING, 0.45F, 1.25F); }

    /** UI_WARNING */
    public static void warning(Player player) { play(player, Sound.NOTE_BASS, 0.45F, 1.05F); }

    /** UI_ERROR */
    public static void error(Player player) { play(player, Sound.NOTE_BASS, 0.55F, 0.7F); }

    private static void play(Player player, Sound sound, float volume, float pitch) {
        if (player == null || !player.isOnline()) return;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
