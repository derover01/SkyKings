package net.skykings.core.ui;

import org.bukkit.ChatColor;

/** Verbindliche SkyKings UI-Konstanten. Kein Feature soll eigene Farblogik erfinden. */
public final class UiTheme {
    private UiTheme() {}

    public static final ChatColor PRIMARY = ChatColor.AQUA;
    public static final ChatColor TEXT = ChatColor.WHITE;
    public static final ChatColor MUTED = ChatColor.GRAY;
    public static final ChatColor DISABLED = ChatColor.DARK_GRAY;
    public static final ChatColor SUCCESS = ChatColor.GREEN;
    public static final ChatColor WARNING = ChatColor.YELLOW;
    public static final ChatColor DANGER = ChatColor.RED;
    public static final ChatColor LEGENDARY = ChatColor.GOLD;
    public static final ChatColor MYTHIC = ChatColor.LIGHT_PURPLE;

    public static final int NAV_BACK = 45;
    public static final int NAV_HOME = 49;
    public static final int NAV_NEXT = 53;

    public static final String STATUS_ACTIVE = PRIMARY + "ACTIVE";
    public static final String STATUS_READY = SUCCESS + "READY";
    public static final String STATUS_LOCKED = DISABLED + "LOCKED";
    public static final String STATUS_COMPLETED = SUCCESS + "COMPLETED";
    public static final String STATUS_COOLDOWN = WARNING + "COOLDOWN";
    public static final String STATUS_CONTESTED = DANGER + "CONTESTED";
    public static final String STATUS_CLAIMED = SUCCESS + "CLAIMED";

    public static String title(String name) {
        return DISABLED.toString() + "SkyKings " + ChatColor.DARK_GRAY + "| " + PRIMARY + name;
    }
}
